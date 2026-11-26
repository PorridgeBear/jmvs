package com.adcworks.jmvs.core.io;

import com.adcworks.jmvs.core.chem.ElementTable;
import com.adcworks.jmvs.core.model.SecondaryStructure;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.core.model.StructureBuilder;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.Column;
import org.rcsb.cif.model.ValueKind;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.StructConf;
import org.rcsb.cif.schema.mm.StructSheetRange;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads PDBx/mmCIF (text) and BinaryCIF with <a href="https://github.com/rcsb/ciftools-java">ciftools-java</a>.
 * Uses the first data block; keeps the first alternate location of each atom.
 */
public final class CifStructureReader implements StructureReader {

    private final ElementTable elements;

    public CifStructureReader(ElementTable elements) {
        this.elements = elements;
    }

    @Override
    public boolean supports(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        return name.endsWith(".cif") || name.endsWith(".mmcif") || name.endsWith(".bcif");
    }

    @Override
    public Structure read(InputStream in, String source) throws StructureReadException {
        MmCifBlock block;
        try {
            MmCifFile file = CifIO.readFromInputStream(in).as(StandardSchemata.MMCIF);
            if (file.getBlocks().isEmpty()) {
                throw new StructureReadException(source, "No data block found");
            }
            block = file.getFirstBlock();
        } catch (IOException | RuntimeException e) {
            throw new StructureReadException(source, "Not a valid mmCIF file: " + e.getMessage(), e);
        }

        AtomSite atomSite = block.getAtomSite();
        if (!atomSite.isDefined() || atomSite.getRowCount() == 0) {
            throw new StructureReadException(source, "No atoms (_atom_site) found");
        }
        StructureBuilder builder = new StructureBuilder(source);
        try {
            addAtoms(builder, atomSite);
            addHelicesAndTurns(builder, block.getStructConf());
            addSheets(builder, block.getStructSheetRange());
        } catch (RuntimeException e) {
            throw new StructureReadException(source, "Invalid atom or secondary structure data: " + e.getMessage(), e);
        }
        return builder.build();
    }

    private void addAtoms(StructureBuilder builder, AtomSite site) {
        Map<String, String> firstAltLoc = new HashMap<>();
        for (int i = 0; i < site.getRowCount(); i++) {
            String chainId = site.getLabelAsymId().get(i);
            String residueName = site.getLabelCompId().get(i);
            String atomName = site.getLabelAtomId().get(i);
            int model = present(site.getPdbxPDBModelNum(), i) ? site.getPdbxPDBModelNum().get(i) : 1;
            Integer labelSeqId = present(site.getLabelSeqId(), i) ? site.getLabelSeqId().get(i) : null;
            int authSeqId = present(site.getAuthSeqId(), i) ? site.getAuthSeqId().get(i) : 0;
            String insertionCode = present(site.getPdbxPDBInsCode(), i) ? site.getPdbxPDBInsCode().get(i) : "";

            if (present(site.getLabelAltId(), i)) {
                String atomKey = model + "|" + chainId + "|" + labelSeqId + "|" + authSeqId + insertionCode + "|"
                        + residueName + "|" + atomName;
                String altLoc = site.getLabelAltId().get(i);
                if (!firstAltLoc.computeIfAbsent(atomKey, k -> altLoc).equals(altLoc)) {
                    continue;
                }
            }

            String authChainId = present(site.getAuthAsymId(), i) ? site.getAuthAsymId().get(i) : chainId;
            float bFactor = present(site.getBIsoOrEquiv(), i) ? (float) site.getBIsoOrEquiv().get(i) : Float.NaN;
            builder.addAtom(model, chainId, authChainId, labelSeqId, authSeqId, insertionCode, residueName,
                    site.getId().get(i), atomName, elements.bySymbol(site.getTypeSymbol().get(i)),
                    (float) site.getCartnX().get(i), (float) site.getCartnY().get(i),
                    (float) site.getCartnZ().get(i), "HETATM".equals(site.getGroupPDB().get(i)), bFactor);
        }
    }

    private static void addHelicesAndTurns(StructureBuilder builder, StructConf conf) {
        if (!conf.isDefined()) {
            return;
        }
        for (int i = 0; i < conf.getRowCount(); i++) {
            String type = conf.getConfTypeId().get(i).toUpperCase(Locale.ROOT);
            Optional<SecondaryStructure> ss = type.startsWith("HELX") ? Optional.of(SecondaryStructure.HELIX)
                    : type.startsWith("TURN") ? Optional.of(SecondaryStructure.TURN)
                    : type.startsWith("STRN") ? Optional.of(SecondaryStructure.SHEET)
                    : Optional.empty();
            if (ss.isPresent() && present(conf.getBegLabelSeqId(), i) && present(conf.getEndLabelSeqId(), i)) {
                builder.addSecondaryStructure(conf.getBegLabelAsymId().get(i), conf.getBegLabelSeqId().get(i),
                        conf.getEndLabelSeqId().get(i), ss.get());
            }
        }
    }

    private static void addSheets(StructureBuilder builder, StructSheetRange sheets) {
        if (!sheets.isDefined()) {
            return;
        }
        for (int i = 0; i < sheets.getRowCount(); i++) {
            if (present(sheets.getBegLabelSeqId(), i) && present(sheets.getEndLabelSeqId(), i)) {
                builder.addSecondaryStructure(sheets.getBegLabelAsymId().get(i), sheets.getBegLabelSeqId().get(i),
                        sheets.getEndLabelSeqId().get(i), SecondaryStructure.SHEET);
            }
        }
    }

    private static boolean present(Column<?> column, int row) {
        return column.isDefined() && column.getValueKind(row) == ValueKind.PRESENT;
    }
}
