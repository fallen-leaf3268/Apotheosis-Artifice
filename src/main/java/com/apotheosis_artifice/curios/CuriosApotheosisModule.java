package com.apotheosis_artifice.curios;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import com.apotheosis_artifice.ApotheosisConfig;

import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.EffectiveSide;

public final class CuriosApotheosisModule {
    private static final Map<ICondition.IContext, Snapshot> resourceStates = new WeakHashMap<>();
    private static volatile Snapshot serverState;
    private static volatile Snapshot clientState;
    private static CurioApotheosisEvents events;

    private CuriosApotheosisModule() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CuriosApotheosisModule::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(CuriosApotheosisModule::onPrepareResources);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CuriosApotheosisModule::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(CuriosApotheosisModule::stopServer);
        MinecraftForge.EVENT_BUS.addListener(CuriosApotheosisModule::serverStopped);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CraftingHelper.register(CuriosModuleCondition.SERIALIZER));
    }

    private static void onPrepareResources(AddReloadListenerEvent event) {
        prepareResources(event.getConditionContext());
    }

    static synchronized Snapshot prepareResources(ICondition.IContext context) {
        return resourceStates.computeIfAbsent(context, key -> serverState == null
            ? new Snapshot(ApotheosisConfig.ENABLE_CURIOS_APOTHEOSIS.get()) : serverState);
    }

    public static synchronized Snapshot resourceState(ICondition.IContext context) {
        return Objects.requireNonNull(resourceStates.get(context), "Curios module resource context was not prepared");
    }

    private static void onServerStarting(ServerAboutToStartEvent event) {
        startServer(event.getServer().getServerResources().managers().getConditionContext());
    }

    static synchronized void startServer(ICondition.IContext context) {
        serverState = resourceState(context);
        updateEvents();
    }

    private static synchronized void stopServer(ServerStoppingEvent event) {
        releaseEvents();
    }

    private static synchronized void serverStopped(ServerStoppedEvent event) {
        serverState = null;
        clientState = null;
        releaseEvents();
    }

    public static boolean isEnabled() {
        Snapshot state = EffectiveSide.get().isClient() ? clientState : serverState;
        return state != null && state.enabled();
    }

    public static boolean isDisabled() {
        Snapshot state = EffectiveSide.get().isClient() ? clientState : serverState;
        return state != null && !state.enabled();
    }

    public static Snapshot serverState() {
        return Objects.requireNonNull(serverState, "Curios module server has not started");
    }

    public static synchronized void acceptServerState(boolean enabled) {
        clientState = new Snapshot(enabled);
        updateEvents();
    }

    public static synchronized void clearClientState() {
        clientState = null;
        updateEvents();
    }

    private static void updateEvents() {
        boolean enabled = Apotheosis.enableAdventure
            && (serverState != null && serverState.enabled() || clientState != null && clientState.enabled());
        if (enabled && events == null) {
            events = new CurioApotheosisEvents();
            MinecraftForge.EVENT_BUS.register(events);
        } else if (!enabled) {
            releaseEvents();
        }
    }

    private static void releaseEvents() {
        if (events == null) return;
        MinecraftForge.EVENT_BUS.unregister(events);
        events.close();
        events = null;
    }

    public record Snapshot(boolean enabled) {}
}
