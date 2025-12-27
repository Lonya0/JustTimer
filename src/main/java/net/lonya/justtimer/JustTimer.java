package net.lonya.justtimer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


@Mod(JustTimer.MODID)
public class JustTimer {
    public static final String MODID = "justtimer";
    private static boolean paused = false;
    private static boolean showing = true;
    private static int stored_time = 0;

    public String tick2time(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void drawGui(GuiGraphics gui, String text) {
        final var mc = Minecraft.getInstance();

        int x = 20, y = 20;
        if (Config.hudPosition == Config.Position.TOP_RIGHT || Config.hudPosition == Config.Position.BOTTOM_RIGHT) {
            int textWidth = mc.font.width(text);
            x = gui.guiWidth() - textWidth - 10;
        }
        if (Config.hudPosition == Config.Position.BOTTOM_RIGHT || Config.hudPosition == Config.Position.BOTTOM_LEFT) {
            y = gui.guiHeight() - mc.font.lineHeight - 10;
        }

        gui.drawString(mc.font,  text, x, y, 0xffffff);
    }

    public JustTimer(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("justtimer")
                        .then(Commands.literal("pause").executes(ctx -> {
                            paused = !paused;
                            return 1;
                        }))
        );
        event.getDispatcher().register(
                Commands.literal("justtimer")
                        .then(Commands.literal("show").executes(ctx -> {
                            showing = !showing;
                            return 1;
                        }))
        );
    }

    @SubscribeEvent
    public void renderGameOverlayEvent(RenderGuiLayerEvent.Post event) {
        if (!showing) return;
        final var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getDebugOverlay().showDebugScreen() || mc.options.hideGui) return;

        final var connection = mc.getConnection();
        if (connection == null) return;

        if (!paused) {
            final var statsRequestPacket = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS);
            connection.send(statsRequestPacket);
            stored_time = mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        }

        final var gui = event.getGuiGraphics();
        final var playtime = tick2time(stored_time);
        drawGui(gui, playtime);
    }
}
