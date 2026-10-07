package com.apotheosis_artifice;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apotheosis_artifice.affix.HeartBaseListener;
import com.apotheosis_artifice.adventure.SigilUpgradeRecipe;
import com.apotheosis_artifice.enchant.ApotheosisArtificeReforgingTableBlock;
import com.apotheosis_artifice.enchant.CleansingRecipe;
import com.apotheosis_artifice.enchant.DiscountBookshelfBlock;
import com.apotheosis_artifice.enchant.EnchantingDiscountRegistry;
import com.apotheosis_artifice.enchant.GemBinderItem;
import com.apotheosis_artifice.enchant.HolyClothItem;
import com.apotheosis_artifice.PortableSalvagingMenu;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantBlockItem;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantMenu;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantTile;
import com.apotheosis_artifice.enchant.MechanicalRavenEnchantingTableBlock;
import com.apotheosis_artifice.enchant.RavenEnchantingTableBlock;
import com.apotheosis_artifice.enchant.RavenEnchantMenu;
import com.apotheosis_artifice.enchant.RavenEnchantTile;
import com.apotheosis_artifice.gemcase.GemCaseBlock;
import com.apotheosis_artifice.gemcase.GemCaseBlockItem;
import com.apotheosis_artifice.gemcase.GemCaseMenu;
import com.apotheosis_artifice.gemcase.GemCaseTile;
import com.apotheosis_artifice.lead.EnderLeadItem;
import com.apotheosis_artifice.lead.EnderLeadTier;
import com.apotheosis_artifice.spawner.ArtificeSpawnerStats;
import com.apotheosis_artifice.spawner.SpawnerPreset;
import com.apotheosis_artifice.spawner.SpawnerPresetRuneItem;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegisterEvent;
import top.theillusivec4.curios.api.CuriosCapability;

@Mod(ApotheosisArtificeMod.MODID)
public class ApotheosisArtificeMod {

    public static final String MODID = "apotheosis_artifice";
    public static final Logger LOGGER = LogManager.getLogger("ApotheosisArtifice");
    public static com.apotheosis_artifice.proxy.IProxy PROXY;

    public static LootCategory CURIO;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Item> GARDEN_FALLBACK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "apotheosis");
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static final RegistryObject<SimpleParticleType> LOOT_PINATA_FIREWORK = PARTICLE_TYPES.register("loot_pinata_firework",
        () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> LOOT_PINATA_SILENT = PARTICLE_TYPES.register("loot_pinata_silent",
        () -> new SimpleParticleType(true));

    public static final RegistryObject<MenuType<PortableSalvagingMenu>> PORTABLE_SALVAGING_MENU = MENU_TYPES.register("portable_salvaging",
        () -> IForgeMenuType.create((id, inv, data) -> new PortableSalvagingMenu(id, inv)));
    public static final RegistryObject<Item> PORTABLE_SALVAGING_TOOL = ITEMS.register("portable_salvaging_tool",
        () -> new PortableSalvagingItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> GEM_CASE_BLOCK = BLOCKS.register("gem_case",
        () -> new GemCaseBlock(Short.MAX_VALUE));
    public static final RegistryObject<Item> GEM_CASE_ITEM = ITEMS.register("gem_case",
        () -> new GemCaseBlockItem(GEM_CASE_BLOCK.get(), new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Block> ENDER_GEM_CASE_BLOCK = BLOCKS.register("ender_gem_case",
        () -> new GemCaseBlock(Integer.MAX_VALUE));
    public static final RegistryObject<Item> ENDER_GEM_CASE_ITEM = ITEMS.register("ender_gem_case",
        () -> new GemCaseBlockItem(ENDER_GEM_CASE_BLOCK.get(), new Item.Properties().stacksTo(64)));
    public static final RegistryObject<MenuType<GemCaseMenu>> GEM_CASE_MENU = MENU_TYPES.register("gem_case",
        () -> IForgeMenuType.create((id, inv, buf) -> new GemCaseMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<BlockEntityType<GemCaseTile>> GEM_CASE_TILE = TILE_TYPES.register("gem_case",
        () -> BlockEntityType.Builder.<GemCaseTile>of(GemCaseTile.BasicGemCaseTile::new, GEM_CASE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<GemCaseTile>> ENDER_GEM_CASE_TILE = TILE_TYPES.register("ender_gem_case",
        () -> BlockEntityType.Builder.<GemCaseTile>of(GemCaseTile.AdvancedGemCaseTile::new, ENDER_GEM_CASE_BLOCK.get()).build(null));

    public static final RegistryObject<Block> LAPIS_SHELF = BLOCKS.register("lapis_shelf",
        DiscountBookshelfBlock::new);
    public static final RegistryObject<Item> LAPIS_SHELF_ITEM = ITEMS.register("lapis_shelf",
        () -> new BlockItem(LAPIS_SHELF.get(), new Item.Properties()));
    public static final RegistryObject<Block> KNOWLEDGE_OVERFLOW_SHELF = BLOCKS.register("knowledge_overflow_shelf",
        DiscountBookshelfBlock::new);
    public static final RegistryObject<Item> KNOWLEDGE_OVERFLOW_SHELF_ITEM = ITEMS.register("knowledge_overflow_shelf",
        () -> new BlockItem(KNOWLEDGE_OVERFLOW_SHELF.get(), new Item.Properties()));

    public static final RegistryObject<Block> RAVEN_ENCHANTING_TABLE = BLOCKS.register("raven_enchanting_table",
        () -> new RavenEnchantingTableBlock());
    public static final RegistryObject<Item> RAVEN_ENCHANTING_TABLE_ITEM = ITEMS.register("raven_enchanting_table",
        () -> new BlockItem(RAVEN_ENCHANTING_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Block> MECHANICAL_RAVEN_TABLE = BLOCKS.register("mechanical_raven_enchanting_table",
        () -> new MechanicalRavenEnchantingTableBlock());
    public static final RegistryObject<Item> MECHANICAL_RAVEN_TABLE_ITEM = ITEMS.register("mechanical_raven_enchanting_table",
        () -> new MechanicalRavenEnchantBlockItem(MECHANICAL_RAVEN_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<MenuType<RavenEnchantMenu>> RAVEN_ENCHANTING_TABLE_MENU = MENU_TYPES.register("raven_enchanting_table",
        () -> {
            var type = IForgeMenuType.<RavenEnchantMenu>create(RavenEnchantMenu::fromBuf);
            RavenEnchantMenu.TYPE = type;
            return type;
        });
    public static final RegistryObject<MenuType<MechanicalRavenEnchantMenu>> MECHANICAL_RAVEN_TABLE_MENU = MENU_TYPES.register("mechanical_raven_enchanting_table",
        () -> {
            var type = IForgeMenuType.<MechanicalRavenEnchantMenu>create(MechanicalRavenEnchantMenu::fromBuf);
            MechanicalRavenEnchantMenu.TYPE = type;
            return type;
        });
    public static final RegistryObject<BlockEntityType<RavenEnchantTile>> RAVEN_ENCHANTING_TILE = TILE_TYPES.register("raven_enchanting_table",
        () -> {
            var type = BlockEntityType.Builder.<RavenEnchantTile>of(RavenEnchantTile::new,
                RAVEN_ENCHANTING_TABLE.get()).build(null);
            RavenEnchantTile.TYPE = type;
            return type;
        });
    public static final RegistryObject<BlockEntityType<MechanicalRavenEnchantTile>> MECHANICAL_RAVEN_TILE = TILE_TYPES.register("mechanical_raven_enchanting_table",
        () -> {
            var type = BlockEntityType.Builder.<MechanicalRavenEnchantTile>of(MechanicalRavenEnchantTile::new,
                MECHANICAL_RAVEN_TABLE.get()).build(null);
            MechanicalRavenEnchantTile.TYPE = type;
            return type;
        });

    public static final RegistryObject<Item> APOTHEOSIS_CHARM = ITEMS.register("apotheosis_charm",
        () -> new GemBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SIGIL_OF_CLEANSING = ITEMS.register("sigil_of_cleansing",
        () -> new dev.shadowsoffire.apotheosis.util.TooltipItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SIGIL_OF_SUPREMACY = ITEMS.register("sigil_of_supremacy",
        () -> new dev.shadowsoffire.apotheosis.util.TooltipItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SIGIL_OF_MALICE = ITEMS.register("sigil_of_malice",
        () -> new dev.shadowsoffire.apotheosis.util.TooltipItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> OCCULT_ENDER_LEAD = ITEMS.register("occult_ender_lead",
        () -> new EnderLeadItem(new Item.Properties().rarity(Rarity.RARE), EnderLeadTier.OCCULT));

    public static final RegistryObject<Item> HOLY_CLOTH = ITEMS.register("holy_cloth",
        () -> new HolyClothItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FRONTIER_SPAWNER_UPGRADE_RUNE = ITEMS.register("frontier_spawner_upgrade_rune",
        () -> new SpawnerPresetRuneItem(new Item.Properties().rarity(Rarity.UNCOMMON), SpawnerPreset.FRONTIER));
    public static final RegistryObject<Item> ASCENT_SPAWNER_UPGRADE_RUNE = ITEMS.register("ascent_spawner_upgrade_rune",
        () -> new SpawnerPresetRuneItem(new Item.Properties().rarity(Rarity.RARE), SpawnerPreset.ASCENT));
    public static final RegistryObject<Item> SUMMIT_SPAWNER_UPGRADE_RUNE = ITEMS.register("summit_spawner_upgrade_rune",
        () -> new SpawnerPresetRuneItem(new Item.Properties().rarity(Rarity.EPIC), SpawnerPreset.SUMMIT));
    public static final RegistryObject<Item> PINNACLE_SPAWNER_UPGRADE_RUNE = ITEMS.register("pinnacle_spawner_upgrade_rune",
        () -> new SpawnerPresetRuneItem(new Item.Properties().rarity(Rarity.EPIC), SpawnerPreset.PINNACLE));

    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<?>> CLEANSING_SERIALIZER = RECIPE_SERIALIZERS.register("cleansing",
        () -> CleansingRecipe.Serializer.INSTANCE);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<?>> SUPREMACY_SERIALIZER = RECIPE_SERIALIZERS.register("supremacy",
        () -> new SigilUpgradeRecipe.Serializer(SigilUpgradeRecipe.Operation.SUPREMACY, SIGIL_OF_SUPREMACY));
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<?>> MALICE_SERIALIZER = RECIPE_SERIALIZERS.register("malice",
        () -> new SigilUpgradeRecipe.Serializer(SigilUpgradeRecipe.Operation.MALICE, SIGIL_OF_MALICE));

    public static final RegistryObject<Block> APOTHEOSIS_REFORGING_TABLE = BLOCKS.register("apotheosis_reforging_table",
        () -> new ApotheosisArtificeReforgingTableBlock());
    public static final RegistryObject<Item> APOTHEOSIS_REFORGING_TABLE_ITEM = ITEMS.register("apotheosis_reforging_table",
        () -> new BlockItem(APOTHEOSIS_REFORGING_TABLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> TAB_APOTHEOSIS_ARTIFICE = CREATIVE_TABS.register("tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.apotheosis_artifice"))
            .icon(() -> new ItemStack(MECHANICAL_RAVEN_TABLE_ITEM.get()))
            .displayItems((params, output) -> {
                if (dev.shadowsoffire.apotheosis.Apotheosis.enableAdventure) {
                    output.accept(PORTABLE_SALVAGING_TOOL.get());
                    output.accept(APOTHEOSIS_CHARM.get());
                    output.accept(APOTHEOSIS_REFORGING_TABLE_ITEM.get());
                    output.accept(SIGIL_OF_CLEANSING.get());
                    output.accept(SIGIL_OF_SUPREMACY.get());
                    output.accept(SIGIL_OF_MALICE.get());
                    output.accept(GEM_CASE_ITEM.get());
                    output.accept(ENDER_GEM_CASE_ITEM.get());
                }
                if (dev.shadowsoffire.apotheosis.Apotheosis.enableEnch) {
                    output.accept(LAPIS_SHELF_ITEM.get());
                    output.accept(KNOWLEDGE_OVERFLOW_SHELF_ITEM.get());
                    output.accept(RAVEN_ENCHANTING_TABLE_ITEM.get());
                    output.accept(MECHANICAL_RAVEN_TABLE_ITEM.get());
                    output.accept(HOLY_CLOTH.get());
                }
                if (dev.shadowsoffire.apotheosis.Apotheosis.enableSpawner) {
                    output.accept(FRONTIER_SPAWNER_UPGRADE_RUNE.get());
                    output.accept(ASCENT_SPAWNER_UPGRADE_RUNE.get());
                    output.accept(SUMMIT_SPAWNER_UPGRADE_RUNE.get());
                    output.accept(PINNACLE_SPAWNER_UPGRADE_RUNE.get());
                }
                if (dev.shadowsoffire.apotheosis.Apotheosis.enableGarden) {
                    output.accept(OCCULT_ENDER_LEAD.get());
                }
            })
            .build());

    @SuppressWarnings("deprecation")
    public ApotheosisArtificeMod() {
        PROXY = net.minecraftforge.fml.DistExecutor.safeRunForDist(
            () -> com.apotheosis_artifice.proxy.ClientProxy::new,
            () -> com.apotheosis_artifice.proxy.ServerProxy::new);
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ApotheosisConfig.init();
        ApotheosisNetwork.init();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        if (!dev.shadowsoffire.apotheosis.Apotheosis.enableGarden) {
            GARDEN_FALLBACK_ITEMS.register("ender_lead", dev.shadowsoffire.apotheosis.garden.EnderLeadItem::new);
            GARDEN_FALLBACK_ITEMS.register(modBus);
        }
        MENU_TYPES.register(modBus);
        TILE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        CREATIVE_TABS.register(modBus);
        MinecraftForge.EVENT_BUS.register(new ApotheosisEvents());
        HeartBaseListener.init();
    }

    private static final List<String> DEFAULT_SLOTS = Arrays.asList(
        "ring", "necklace", "charm", "belt", "hands", "bracelet",
        "hostility_curse", "pandora_charm");

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (dev.shadowsoffire.apotheosis.Apotheosis.enableEnch) EnchantingDiscountRegistry.INSTANCE.registerToBus();
            if (dev.shadowsoffire.apotheosis.Apotheosis.enableSpawner) ArtificeSpawnerStats.register();
            CURIO = LootCategory.register(null, "curio",
                stack -> !stack.isEmpty() && stack.getCapability(CuriosCapability.ITEM).isPresent(),
                new EquipmentSlot[] { EquipmentSlot.CHEST });

            for (String slot : DEFAULT_SLOTS) {
                TagKey<Item> tag = ItemTags.create(new ResourceLocation("curios:" + slot));
                LootCategory.register(LootCategory.HELMET, "curios:" + slot,
                    s -> !s.isEmpty() && s.is(tag),
                    new EquipmentSlot[] { EquipmentSlot.CHEST });
            }
        });
    }
}
