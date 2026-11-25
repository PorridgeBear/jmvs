package com.adcworks.jmvs.render;

import com.adcworks.jmvs.scene.Picking;
import com.adcworks.jmvs.scene.Scene;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.collision.Ray;

import java.util.OptionalInt;

/**
 * Converts a screen position to a ray and finds the nearest atom in the scene's pick targets.
 */
public final class ScreenPicker {

    private ScreenPicker() {
    }

    public static OptionalInt pick(Camera camera, Scene scene, int screenX, int screenY) {
        if (scene == null) {
            return OptionalInt.empty();
        }
        Ray ray = camera.getPickRay(screenX, screenY);
        return Picking.pick(scene.pickTargets(),
                new float[]{ray.origin.x, ray.origin.y, ray.origin.z},
                new float[]{ray.direction.x, ray.direction.y, ray.direction.z});
    }
}
