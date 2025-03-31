package com.aicompanion.mod.init;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.entity.MiniBuildEntity;
import com.aicompanion.mod.entity.renderer.AICompanionRenderer;
import com.aicompanion.mod.entity.renderer.MiniBuildRenderer;
import com.aicompanion.mod.item.AICompanionSpawnEggItem;
import com.aicompanion.mod.item.MiniBuildCreatorItem;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AICompanionMod.MOD_ID, bus = Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITIES, AICompanionMod.MOD_ID);
    
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, AICompanionMod.MOD_ID);

    // Entity registration
    public static final RegistryObject<EntityType<AICompanionEntity>> AI_COMPANION = 
            ENTITY_TYPES.register("ai_companion", 
                    () -> EntityType.Builder.<AICompanionEntity>of(
                            AICompanionEntity::new, 
                            EntityClassification.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("ai_companion"));
    
    // Mini Build Entity registration
    public static final RegistryObject<EntityType<MiniBuildEntity>> MINI_BUILD = 
            ENTITY_TYPES.register("mini_build", 
                    () -> EntityType.Builder.<MiniBuildEntity>of(
                            MiniBuildEntity::new, 
                            EntityClassification.MISC)
                    .sized(0.5F, 0.5F) // Small hitbox size
                    .clientTrackingRange(64) // Visible from further away
                    .build("mini_build"));

    // Spawn egg registration
    public static final RegistryObject<Item> AI_COMPANION_SPAWN_EGG = 
            ITEMS.register("ai_companion_spawn_egg", 
                    () -> new AICompanionSpawnEggItem(
                            AI_COMPANION,
                            0x4287f5, // Primary color (blue)
                            0xf54242, // Secondary color (red)
                            new Item.Properties().tab(ItemGroup.TAB_MISC)));
    
    // Mini Build Creator Item registration
    public static final RegistryObject<Item> MINI_BUILD_CREATOR = 
            ITEMS.register("mini_build_creator", 
                    () -> new MiniBuildCreatorItem(
                            new Item.Properties()
                            .tab(ItemGroup.TAB_TOOLS)
                            .stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        ITEMS.register(eventBus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(AI_COMPANION.get(), createAttributes().build());
        // The MiniBuild entity doesn't need attributes since it's not a living entity
    }

    public static void registerEntityAttributes() {
        // This is called from the main mod class during common setup
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return AICompanionEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(AI_COMPANION.get(), AICompanionRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(MINI_BUILD.get(), MiniBuildRenderer::new);
    }
}
