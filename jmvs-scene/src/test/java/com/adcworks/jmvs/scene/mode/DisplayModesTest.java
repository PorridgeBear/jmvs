package com.adcworks.jmvs.scene.mode;

import com.adcworks.jmvs.core.model.Atom;
import com.adcworks.jmvs.core.model.Structure;
import com.adcworks.jmvs.scene.Registry;
import com.adcworks.jmvs.scene.Samples;
import com.adcworks.jmvs.scene.Scene;
import com.adcworks.jmvs.scene.SphereInstances;
import com.adcworks.jmvs.scene.TriangleMesh;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayModesTest {

    private static final Structure CRAMBIN = Samples.read("1crn.cif");

    @Test
    void ballsUseUnitedAtomRadiiWithoutHydrogens() {
        Scene scene = new BallsMode().build(CRAMBIN);
        SphereInstances spheres = scene.spheres();

        assertThat(spheres.count()).isEqualTo(CRAMBIN.atoms().size());
        Atom first = CRAMBIN.atoms().getFirst();
        assertThat(first.element().symbol()).isEqualTo("N");
        assertThat(spheres.radius(0)).isEqualTo(1.507f);
        assertThat(scene.cylinders().count()).isZero();
        assertThat(scene.pickTargets()).isSameAs(spheres);
    }

    @Test
    void sticksDrawTwoHalfCylindersPerBond() {
        Scene scene = new SticksMode().build(CRAMBIN);

        assertThat(scene.cylinders().count()).isEqualTo(CRAMBIN.bonds().size() * 2);
        var bond = CRAMBIN.bonds().getFirst();
        float[] d = scene.cylinders().data();
        Atom a = CRAMBIN.atoms().get(bond.atom1());
        Atom b = CRAMBIN.atoms().get(bond.atom2());
        assertThat(d[0]).isEqualTo(a.x());
        assertThat(d[3]).isEqualTo((a.x() + b.x()) / 2);
        assertThat(scene.cylinders().atoms()[0]).isEqualTo(a.index());
        assertThat(scene.cylinders().atoms()[1]).isEqualTo(b.index());
    }

    @Test
    void ballsAndSticksUseSmallerBalls() {
        Scene scene = new BallsAndSticksMode().build(CRAMBIN);

        assertThat(scene.spheres().radius(0)).isEqualTo(1.507f * BallsAndSticksMode.BALL_SCALE);
        assertThat(scene.cylinders().count()).isEqualTo(CRAMBIN.bonds().size() * 2);
    }

    @Test
    void ribbonsFollowAlphaCarbonsOfEachChain() {
        Structure structure = Samples.read("1usc.cif");
        Scene scene = new RibbonsMode().build(structure);

        assertThat(scene.meshes()).hasSize(2);
        for (TriangleMesh mesh : scene.meshes()) {
            assertThat(Arrays.stream(mesh.indices()).boxed()).allSatisfy(i -> assertThat(i).isBetween(0, mesh.vertexCount() - 1));
            assertThat(Arrays.stream(normalLengths(mesh)).boxed()).allSatisfy(l -> assertThat(l).isBetween(0.99, 1.01));
            Set<String> names = Arrays.stream(mesh.atoms()).mapToObj(i -> structure.atoms().get(i).name())
                    .collect(Collectors.toSet());
            assertThat(names).containsExactly("CA");
        }
        assertThat(scene.pickTargets().count()).isGreaterThan(300);
    }

    @Test
    void ribbonsStayNearTheBackbone() {
        Scene scene = new RibbonsMode().build(CRAMBIN);
        float[] positions = scene.meshes().getFirst().positions();
        var sphere = CRAMBIN.boundingSphere();

        for (int i = 0; i < positions.length; i += 3) {
            double dx = positions[i] - sphere.x(), dy = positions[i + 1] - sphere.y(), dz = positions[i + 2] - sphere.z();
            assertThat(Math.sqrt(dx * dx + dy * dy + dz * dz)).isLessThan(sphere.radius() + 5);
        }
    }

    @Test
    void ribbonsDoNotSupportCpk() {
        Registry registry = Registry.load();
        DisplayMode ribbons = registry.mode("ribbons");

        assertThat(ribbons.supports(registry.scheme("cpk"))).isFalse();
        assertThat(registry.schemeFor(ribbons, registry.scheme("cpk")).id()).isEqualTo("chain");
        assertThat(registry.schemeFor(ribbons, registry.scheme("structure")).id()).isEqualTo("structure");
    }

    private static double[] normalLengths(TriangleMesh mesh) {
        float[] n = mesh.normals();
        double[] lengths = new double[n.length / 3];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = Math.sqrt(n[i * 3] * n[i * 3] + n[i * 3 + 1] * n[i * 3 + 1] + n[i * 3 + 2] * n[i * 3 + 2]);
        }
        return lengths;
    }
}
