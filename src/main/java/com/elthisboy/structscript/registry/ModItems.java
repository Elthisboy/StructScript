package com.elthisboy.structscript.registry;

import com.elthisboy.structscript.StructScript;
import com.elthisboy.structscript.item.StructureBlueprintItem;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item STRUCTURE_BLUEPRINT =
            new StructureBlueprintItem(new Item.Settings().maxCount(64));

    public static void register() {

        Registry.register(
                Registries.ITEM,
                Identifier.of(StructScript.MOD_ID, "structure_blueprint"),
                STRUCTURE_BLUEPRINT
        );

    }
}