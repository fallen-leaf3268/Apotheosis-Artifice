package com.apotheosis_artifice;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apotheosis_artifice.enchant.ApotheosisArtificeReforgingTableBlock;
import com.apotheosis_artifice.enchant.GemBinderItem;
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

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
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
import top.theillusivec4.curios.api.CuriosCapability;

@Mod(ApotheosisArtificeMod.MODID)
public class ApotheosisArtificeMod {

    public static final String MODID = "apotheosis_artifice";
    public static final Logger LOGGER = LogManager.getLogger("ApotheosisArtifice");
    public static com.apotheosis_artifice.proxy.IProxy PROXY;

    public static LootCategory CURIO;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // === 便携回收工具 ===
    public static final RegistryObject<MenuType<PortableSalvagingMenu>> PORTABLE_SALVAGING_MENU = MENU_TYPES.register("portable_salvaging",
        () -> IForgeMenuType.create((id, inv, data) -> new PortableSalvagingMenu(id, inv)));
    public static final RegistryObject<Item> PORTABLE_SALVAGING_TOOL = ITEMS.register("portable_salvaging_tool",
        () -> new PortableSalvagingItem(new Item.Properties().stacksTo(1)));

    // === 宝石柜 ===
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

    // === 渡鸦附魔台 ===
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

    // === 宝石绑定器 ===
    public static final RegistryObject<Item> APOTHEOSIS_CHARM = ITEMS.register("apotheosis_charm",
        () -> new GemBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // === 高级重铸台 ===
    public static final RegistryObject<Block> APOTHEOSIS_REFORGING_TABLE = BLOCKS.register("apotheosis_reforging_table",
        () -> new ApotheosisArtificeReforgingTableBlock());
    public static final RegistryObject<Item> APOTHEOSIS_REFORGING_TABLE_ITEM = ITEMS.register("apotheosis_reforging_table",
        () -> new BlockItem(APOTHEOSIS_REFORGING_TABLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // === 创造物品栏（必须在所有物品之后声明） ===
    public static final RegistryObject<CreativeModeTab> TAB_APOTHEOSIS_ARTIFICE = CREATIVE_TABS.register("tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.apotheosis_artifice"))
            .icon(() -> new ItemStack(MECHANICAL_RAVEN_TABLE_ITEM.get()))
            .displayItems((params, output) -> {
                output.accept(PORTABLE_SALVAGING_TOOL.get());
                output.accept(RAVEN_ENCHANTING_TABLE_ITEM.get());
                output.accept(MECHANICAL_RAVEN_TABLE_ITEM.get());
                output.accept(APOTHEOSIS_CHARM.get());
                output.accept(APOTHEOSIS_REFORGING_TABLE_ITEM.get());
                output.accept(GEM_CASE_ITEM.get());
                output.accept(ENDER_GEM_CASE_ITEM.get());
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
        MENU_TYPES.register(modBus);
        TILE_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        CREATIVE_TABS.register(modBus);
        MinecraftForge.EVENT_BUS.register(new ApotheosisEvents());
    }

    private static final List<String> DEFAULT_SLOTS = Arrays.asList(
        "ring", "necklace", "charm", "belt", "hands", "bracelet",
        "hostility_curse", "pandora_charm");

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CURIO = LootCategory.register(null, "curio",
                stack -> !stack.isEmpty() && stack.getCapability(CuriosCapability.ITEM).isPresent(),
                new EquipmentSlot[] { EquipmentSlot.CHEST });

            for (String slot : DEFAULT_SLOTS) {
                TagKey<Item> tag = ItemTags.create(new ResourceLocation("curios:" + slot));
                LootCategory.register(LootCategory.HELMET, "curios:" + slot,
                    s -> !s.isEmpty() && s.is(tag),
                    new EquipmentSlot[] { EquipmentSlot.CHEST });
            }

            LOGGER.info("Registered curio + {} slot-specific LootCategories (curios:xxx)", DEFAULT_SLOTS.size());
        });
    }
}
