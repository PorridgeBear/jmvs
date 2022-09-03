package com.adcworks.jmvs.io.cif;

import com.adcworks.jmvs.io.MoleculeReader;
import com.adcworks.jmvs.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Reader for <a href="https://mmcif.wwpdb.org/pdbx-mmcif-home-page.html">PDBx/mmCIF</a> format.
 */
public class CIFReader implements MoleculeReader
{
    private List<StructSheetRange> structSheetRanges = new ArrayList<>();
    private List<StructConf> structConfs = new ArrayList<>();
    private Molecule molecule;

    /**
     * Read a molecule file.
     * @param file
     * @return
     */
    public Molecule read(File file)
    {
        // TODO: validate the file is a valid pdbx/mmcif

        System.out.println("Reading CIF: " + file.getName());

        molecule = new Molecule();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            molecule.setName(file.getName());

            String line = "";
            boolean isLoop = false;
            boolean isCategoryOfInterest = false;
            boolean isTermination = false;

            Map<String, Integer> categoryIndices = new HashMap<>();
            int index = 0;

            while((line = reader.readLine()) != null) {
                line = line.trim();

                isLoop = line.startsWith("loop_");
                isCategoryOfInterest =
                        line.startsWith("_struct_sheet_range.") ||
                        line.startsWith("_struct_conf.") ||
                        line.startsWith("_atom_site.");
                isTermination = !isCategoryOfInterest && (
                        line.startsWith("_") || line.startsWith("#") || isLoop
                );

                if (isCategoryOfInterest) {
                    categoryIndices.put(line, index++);
                    System.out.println(line);
                } else if (!isTermination && categoryIndices.size() > 0) {
                    String[] data = line.split("\\s+");
                    System.out.println(line);
                    String categoryKey = (String) categoryIndices.keySet().toArray()[0];
                    String methodName = categoryKey.split("\\.")[0];
                    Method method = this.getClass().getDeclaredMethod(methodName,
                            Molecule.class, String[].class, Map.class);
                    method.invoke(this, molecule, data, categoryIndices);
                } else {
                    categoryIndices.clear();
                    index = 0;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load the molecule.");
            System.exit(0);
        }

        System.out.println(String.format("Adding %d sheets", structSheetRanges.size()));
        for (StructSheetRange sheetRange : structSheetRanges) {
            molecule.addSheet(
                    sheetRange.sheet_id,
                    Integer.parseInt(sheetRange.beg_label_seq_id),
                    Integer.parseInt(sheetRange.end_label_seq_id)
            );
        }

        System.out.println(String.format("Adding %d helices", structConfs.size()));
        for (StructConf structConf : structConfs) {
            molecule.addHelix(
                    structConf.conf_type_id,
                    Integer.parseInt(structConf.beg_label_seq_id),
                    Integer.parseInt(structConf.end_label_seq_id)
            );
        }

        molecule.finish();

        return molecule;
    }

    private void _struct_sheet_range(Molecule molecule, String[] data, Map<String, Integer> categoryIndices)
    {
        StructSheetRange structSheetRange = new StructSheetRange();
        structSheetRange.sheet_id = data[categoryIndices.get("_struct_sheet_range.sheet_id")];
        structSheetRange.id = data[categoryIndices.get("_struct_sheet_range.id")];
        structSheetRange.beg_label_seq_id = data[categoryIndices.get("_struct_sheet_range.beg_label_seq_id")];
        structSheetRange.end_label_seq_id = data[categoryIndices.get("_struct_sheet_range.end_label_seq_id")];
        System.out.println(structSheetRange);

        structSheetRanges.add(structSheetRange);
    }

    private void _struct_conf(Molecule molecule, String[] data, Map<String, Integer> categoryIndices)
    {
        StructConf structConf = new StructConf();
        structConf.conf_type_id = data[categoryIndices.get("_struct_conf.conf_type_id")];
        structConf.id = data[categoryIndices.get("_struct_conf.id")];
        structConf.beg_label_seq_id = data[categoryIndices.get("_struct_conf.beg_label_seq_id")];
        structConf.end_label_seq_id = data[categoryIndices.get("_struct_conf.end_label_seq_id")];
        System.out.println(structConf);

        structConfs.add(structConf);
    }

    /**
     * Parse atoms into the molecule.
     * @param molecule
     * @param data
     * @param categoryIndices
     */
    private void _atom_site(Molecule molecule, String[] data, Map<String, Integer> categoryIndices)
    {
        int modelNum = Integer.parseInt(data[categoryIndices.get("_atom_site.pdbx_PDB_model_num")]);

        String id = data[categoryIndices.get("_atom_site.id")];
        String typeSymbol = data[categoryIndices.get("_atom_site.type_symbol")];
        String labelAtomId = data[categoryIndices.get("_atom_site.label_atom_id")];
        String labelCompId = data[categoryIndices.get("_atom_site.label_comp_id")];
        String labelAsymId = data[categoryIndices.get("_atom_site.label_asym_id")];
        String labelEntityId = data[categoryIndices.get("_atom_site.label_entity_id")];
        String labelSeqId = data[categoryIndices.get("_atom_site.label_seq_id")];
        float x = Float.parseFloat(data[categoryIndices.get("_atom_site.Cartn_x")]);
        float y = Float.parseFloat(data[categoryIndices.get("_atom_site.Cartn_y")]);
        float z = Float.parseFloat(data[categoryIndices.get("_atom_site.Cartn_z")]);

        Element element = ElementsManager.getInstance().getElement(typeSymbol);

        Atom atom = new Atom(id, typeSymbol, labelAtomId, x, y, z);
        atom.setIsHeterogen(data[0].equals("HETATM"));
        atom.setCovalentRadius(element.getCovalentRadius());

        molecule.addAtom(modelNum, labelAsymId, labelSeqId, labelCompId, atom);
    }

    /**
     * Data items in the STRUCT_SHEET_RANGE category record details about the residue ranges that
     * form a beta-sheet. Residues are included in a range if they made beta-sheet-type hydrogen-bonding
     * interactions with at least one adjacent strand and if there are at least two residues in the range.
     */
    static class StructSheetRange {
        public String sheet_id;
        public String id;
        public String beg_label_seq_id;
        public String end_label_seq_id;

        public String toString() {
            return String.format("Sheet ID: %s ID: %s Beg: %s End: %s",
                    sheet_id, id, beg_label_seq_id, end_label_seq_id);
        }
    }

    /**
     * Data items in the STRUCT_CONF category record details about the backbone conformation of a segment of polymer.
     */
    static class StructConf {

        public String conf_type_id;
        public String id;
        public String beg_label_seq_id;
        public String end_label_seq_id;

        public String toString() {
            return String.format("Conf ID: %s ID: %s Beg: %s End: %s",
                    conf_type_id, id, beg_label_seq_id, end_label_seq_id);
        }
    }
}