package com.elthisboy.structscript;

import com.elthisboy.structscript.command.StructCommand;
import com.elthisboy.structscript.network.RotatePacket;
import com.elthisboy.structscript.registry.ModItems;
import com.elthisboy.structscript.schematic.SchematicManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class StructScript implements ModInitializer {

    public static final String MOD_ID = "structscript";

    @Override
    public void onInitialize() {
        ModItems.register();
        StructCommand.register();
        SchematicManager.get("dummy");
        RotatePacket.register();

        // Crear la carpeta de estructuras si no existe
        try {
            Path structuresDir = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("structscript/structures");
            Files.createDirectories(structuresDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}