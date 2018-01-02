/*
 * MIT License
 * 
 * Copyright (c) 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.devathon.contest2016;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.devathon.contest2016.npc.NPCController;
import org.devathon.contest2016.npc.NPCRegistry;
import org.devathon.contest2016.data.SpawnControl;
import org.devathon.contest2016.util.ItemStackUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Cryptkeeper
 * @since 05.11.2016
 */
public class WorldListener implements Listener {

    private final Map<UUID, Location> respawnLocations = new HashMap<>();

    private boolean running;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        reset(player);

        ItemStack filler = ItemStackUtil.makeCustomItemStack(Material.STAINED_GLASS_PANE, ChatColor.GRAY + "Empty Slot", 15);
        ItemStack machineItem = ItemStackUtil.makeCustomItemStack(Material.IRON_BLOCK, ChatColor.LIGHT_PURPLE + "The Hell Machine " + ChatColor.GRAY + "(Right Click to Place)");

        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, filler);
        }

        player.getInventory().setItem(4, machineItem);
        player.setGameMode(GameMode.SURVIVAL);

        event.setJoinMessage(null);
    }

    @EventHandler
    public void onSwap(PlayerItemHeldEvent event) {
        if (!this.running && event.getNewSlot() != 4) {
            event.getPlayer().getInventory().setHeldItemSlot(4);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
        event.setFoodLevel(20);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(event.toWeatherState());
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        event.setCancelled(event.toThunderState());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        respawnLocations.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());

        event.setDeathMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        event.setDroppedExp(0);
        event.getDrops().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Location location = respawnLocations.remove(player.getUniqueId());

        if (location != null) {
            event.setRespawnLocation(location);

            reset(player);

            Options.KIT_ITEMS.forEach(itemStack -> player.getInventory().addItem(itemStack.clone()));

            updateArmor(player);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "You breathe again..."));
        }
    }

    @EventHandler
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler
    public void onExplode2(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() != null && event.getItem().getType() == Material.FIREBALL) {
                event.getPlayer().launchProjectile(LargeFireball.class);
                if (event.getItem().getAmount() <= 1) {
                    event.getPlayer().getInventory().remove(event.getItem());
                } else {
                    event.getItem().setAmount(event.getItem().getAmount() - 1);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // We'll just assume it's our custom ItemStack.
        if (event.getBlock().getType() == Material.IRON_BLOCK) {

            running = true;

            Location location = event.getBlock().getLocation();
            int size = Options.ARENA_SIZE;

            // Set our Y to where we won't collide with anything.
            location.setY(location.getWorld().getHighestBlockYAt(location));

            for (int x = -size / 2; x <= size / 2; x++) {
                for (int z = -size / 2; z <= size / 2; z++) {
                    int maxY = (x == -size / 2 || z == -size / 2 || x == size / 2 || z == size / 2) ? 5 : 1;

                    for (int y = 0; y < maxY; y++) {
                        Block block = location.clone().add(x, y, z).getBlock();

                        if (y == 0) {
                            block.setType(Material.NETHERRACK);
                        } else {
                            block.setType(Material.IRON_FENCE);
                        }
                    }
                }
            }

            Player player = event.getPlayer();

            player.getInventory().clear();
            player.getActivePotionEffects().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(location.clone().add(5, 1, 0));

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "An arena appears..."));

            Options.KIT_ITEMS.forEach(itemStack -> player.getInventory().addItem(itemStack.clone()));
            
            updateArmor(player);

            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getInstance(), () -> {
                Location spawnLocation = location.clone().add(0, 1, 0);

                SpawnControl point = new SpawnControl(spawnLocation);
                NPCController controller = new NPCController(player.getUniqueId(), point);

                NPCRegistry.getInstance().register(controller);
            });

            event.setBuild(false);
        }
    }

    private void reset(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getEquipment().setArmorContents(new ItemStack[4]);
    }
    
    public static void updateArmor(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isValid(stack)) {
                switch (stack.getType()) {
                    case IRON_HELMET:
                    case GOLD_HELMET:
                    case LEATHER_HELMET:
                    case DIAMOND_HELMET:
                    case CHAINMAIL_HELMET:
                        if (isValid(player.getInventory().getHelmet())) {
                            continue;
                        }

                        player.getInventory().remove(stack);
                        
                        player.getInventory().setHelmet(stack);
                        continue;
                    case IRON_CHESTPLATE:
                    case GOLD_CHESTPLATE:
                    case LEATHER_CHESTPLATE:
                    case DIAMOND_CHESTPLATE:
                    case CHAINMAIL_CHESTPLATE:
                        if (isValid(player.getInventory().getChestplate())) {
                            continue;
                        }

                        player.getInventory().remove(stack);

                        player.getInventory().setChestplate(stack);
                        continue;
                    case IRON_LEGGINGS:
                    case GOLD_LEGGINGS:
                    case LEATHER_LEGGINGS:
                    case DIAMOND_LEGGINGS:
                    case CHAINMAIL_LEGGINGS:
                        if (isValid(player.getInventory().getLeggings())) {
                            continue;
                        }

                        player.getInventory().remove(stack);

                        player.getInventory().setLeggings(stack);
                        continue;
                    case IRON_BOOTS:
                    case GOLD_BOOTS:
                    case LEATHER_BOOTS:
                    case DIAMOND_BOOTS:
                    case CHAINMAIL_BOOTS:
                        if (isValid(player.getInventory().getBoots())) {
                            continue;
                        }

                        player.getInventory().remove(stack);

                        player.getInventory().setBoots(stack);
                        continue;
                    default:
                        // Whatever lol
                }
            }
        }
    }
    
    private static boolean isValid(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR;
    }
}
