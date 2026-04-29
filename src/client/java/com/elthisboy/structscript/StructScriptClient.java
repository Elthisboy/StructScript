package com.elthisboy.structscript;

import com.elthisboy.structscript.input.BlueprintKeyHandler;
import com.elthisboy.structscript.preview.BlueprintPreviewRenderer;
import com.elthisboy.structscript.schematic.SchematicManager;
import net.fabricmc.api.ClientModInitializer;

public class StructScriptClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SchematicManager.clearCache();
        BlueprintPreviewRenderer.register();
        BlueprintKeyHandler.register();
    }
}
