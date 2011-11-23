// $Id$
/*
 * WorldEdit
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.worldedit.bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.blocks.*;
import com.sk89q.worldedit.EntityType;
import com.sk89q.worldedit.regions.Region;

public class BukkitWorld extends LocalWorld {
    private World world;

    /**
     * Construct the object.
     * @param world
     */
    public BukkitWorld(World world) {
        this.world = world;
    }

    /**
     * Get the world handle.
     * 
     * @return
     */
    public World getWorld() {
        return world;
    }

    /**
     * Get the name of the world
     * 
     * @return
     */
    public String getName() {
        return world.getName();
    }

    /**
     * Set block type.
     * 
     * @param pt
     * @param type
     * @return
     */
    @Override
    public boolean setBlockType(Vector pt, int type) {
        return world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).setTypeId(type);
    }

    /**
     * Set block type.
     * 
     * @param pt
     * @param type
     * @return
     */
    @Override
    public boolean setBlockTypeFast(Vector pt, int type) {
        final Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (fastLightingAvailable) {
            type = type & 255;
            final int previousOpacity = Block_lightOpacity[type];
            Block_lightOpacity[type] = 0;
            final boolean ret = block.setTypeId(type, false);
            Block_lightOpacity[type] = previousOpacity;
            return ret;
        }

        return block.setTypeId(type, false);
    }

    /**
     * set block type & data
     * @param pt
     * @param type
     * @param data
     * @return 
     */
    @Override
    public boolean setTypeIdAndData(Vector pt, int type, int data) {
        return world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).setTypeIdAndData(type, (byte) data, true);
    }

    /**
     * set block type & data
     * @param pt
     * @param type
     * @param data
     * @return 
     */
    @Override
    public boolean setTypeIdAndDataFast(Vector pt, int type, int data) {
        final Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (fastLightingAvailable) {
            type = type & 255;
            final int previousOpacity = Block_lightOpacity[type];
            Block_lightOpacity[type] = 0;
            final boolean ret = block.setTypeIdAndData(type, (byte) data, false);
            Block_lightOpacity[type] = previousOpacity;
            return ret;
        }

        return block.setTypeIdAndData(type, (byte) data, false);
    }

    /**
     * Get block type.
     * 
     * @param pt
     * @return
     */
    @Override
    public int getBlockType(Vector pt) {
        return world.getBlockTypeIdAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
    }

    /**
     * Set block data.
     * 
     * @param pt
     * @param data
     */
    @Override
    public void setBlockData(Vector pt, int data) {
        world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).setData((byte) data);
    }

    /**
     * Set block data.
     * 
     * @param pt
     * @param data
     */
    @Override
    public void setBlockDataFast(Vector pt, int data) {
        world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).setData((byte) data, false);
    }

    /**
     * Get block data.
     * 
     * @param pt
     * @return
     */
    @Override
    public int getBlockData(Vector pt) {
        return world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).getData();
    }

    /**
     * Get block light level.
     * 
     * @param pt
     * @return
     */
    @Override
    public int getBlockLightLevel(Vector pt) {
        return world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).getLightLevel();
    }

    /**
     * Regenerate an area.
     * 
     * @param region
     * @param editSession
     * @return
     */
    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        BaseBlock[] history = new BaseBlock[16 * 16 * 128];

        for (Vector2D chunk : region.getChunks()) {
            Vector min = new Vector(chunk.getBlockX() * 16, 0, chunk.getBlockZ() * 16);

            // First save all the blocks inside
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < 128; ++y) {
                    for (int z = 0; z < 16; ++z) {
                        Vector pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;
                        history[index] = editSession.getBlock(pt);
                    }
                }
            }

            try {
                world.regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Then restore 
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < 128; ++y) {
                    for (int z = 0; z < 16; ++z) {
                        Vector pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;

                        // We have to restore the block if it was outside
                        if (!region.contains(pt)) {
                            editSession.smartSetBlock(pt, history[index]);
                        } else { // Otherwise fool with history
                            editSession.rememberChange(pt, history[index],
                                    editSession.rawGetBlock(pt));
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Attempts to accurately copy a BaseBlock's extra data to the world.
     * 
     * @param pt
     * @param block
     * @return
     */
    @Override
    public boolean copyToWorld(Vector pt, BaseBlock block) {
        if (block instanceof SignBlock) {
            // Signs
            setSignText(pt, ((SignBlock) block).getText());
            return true;
        }

        if (block instanceof FurnaceBlock) {
            // Furnaces
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof Furnace)) return false;
            Furnace bukkit = (Furnace) state;
            FurnaceBlock we = (FurnaceBlock) block;
            bukkit.setBurnTime(we.getBurnTime());
            bukkit.setCookTime(we.getCookTime());
            return setContainerBlockContents(pt, ((ContainerBlock) block).getItems());
        }

        if (block instanceof ContainerBlock) {
            // Chests/dispenser
            return setContainerBlockContents(pt, ((ContainerBlock) block).getItems());
        }

        if (block instanceof MobSpawnerBlock) {
            // Mob spawners
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof CreatureSpawner)) return false;
            CreatureSpawner bukkit = (CreatureSpawner) state;
            MobSpawnerBlock we = (MobSpawnerBlock) block;
            bukkit.setCreatureTypeId(we.getMobType());
            bukkit.setDelay(we.getDelay());
            return true;
        }

        if (block instanceof NoteBlock) {
            // Note block
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof org.bukkit.block.NoteBlock)) return false;
            org.bukkit.block.NoteBlock bukkit = (org.bukkit.block.NoteBlock) state;
            NoteBlock we = (NoteBlock) block;
            bukkit.setRawNote(we.getNote());
            return true;
        }

        return false;
    }

    /**
     * Attempts to read a BaseBlock's extra data from the world.
     * 
     * @param pt
     * @param block
     * @return
     */
    @Override
    public boolean copyFromWorld(Vector pt, BaseBlock block) {
        if (block instanceof SignBlock) {
            // Signs
            ((SignBlock) block).setText(getSignText(pt));
            return true;
        }

        if (block instanceof FurnaceBlock) {
            // Furnaces
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof Furnace)) return false;
            Furnace bukkit = (Furnace) state;
            FurnaceBlock we = (FurnaceBlock) block;
            we.setBurnTime(bukkit.getBurnTime());
            we.setCookTime(bukkit.getCookTime());
            ((ContainerBlock) block).setItems(getContainerBlockContents(pt));
            return true;
        }

        if (block instanceof ContainerBlock) {
            // Chests/dispenser
            ((ContainerBlock) block).setItems(getContainerBlockContents(pt));
            return true;
        }

        if (block instanceof MobSpawnerBlock) {
            // Mob spawners
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof CreatureSpawner)) return false;
            CreatureSpawner bukkit = (CreatureSpawner) state;
            MobSpawnerBlock we = (MobSpawnerBlock) block;
            we.setMobType(bukkit.getCreatureTypeId());
            we.setDelay((short) bukkit.getDelay());
            return true;
        }

        if (block instanceof NoteBlock) {
            // Note block
            Block bukkitBlock = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if (bukkitBlock == null) return false;
            BlockState state = bukkitBlock.getState();
            if (!(state instanceof org.bukkit.block.NoteBlock)) return false;
            org.bukkit.block.NoteBlock bukkit = (org.bukkit.block.NoteBlock) state;
            NoteBlock we = (NoteBlock) block;
            we.setNote(bukkit.getRawNote());
        }

        return false;
    }

    /**
     * Clear a chest's contents.
     * 
     * @param pt
     */
    @Override
    public boolean clearContainerBlockContents(Vector pt) {
        Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (block == null) {
            return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof org.bukkit.block.ContainerBlock)) {
            return false;
        }

        org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
        Inventory inven = chest.getInventory();
        inven.clear();
        return true;
    }

    /**
     * Generate a tree at a location.
     * 
     * @param pt
     * @return
     */
    @Override
    public boolean generateTree(EditSession editSession, Vector pt) {
        return world.generateTree(BukkitUtil.toLocation(world, pt), TreeType.TREE,
                new EditSessionBlockChangeDelegate(editSession));
    }

    /**
     * Generate a big tree at a location.
     * 
     * @param pt
     * @return
     */
    @Override
    public boolean generateBigTree(EditSession editSession, Vector pt) {
        return world.generateTree(BukkitUtil.toLocation(world, pt), TreeType.BIG_TREE,
                new EditSessionBlockChangeDelegate(editSession));
    }

    /**
     * Generate a birch tree at a location.
     * 
     * @param pt
     * @return
     */
    @Override
    public boolean generateBirchTree(EditSession editSession, Vector pt) {
        return world.generateTree(BukkitUtil.toLocation(world, pt), TreeType.BIRCH,
                new EditSessionBlockChangeDelegate(editSession));
    }

    /**
     * Generate a redwood tree at a location.
     * 
     * @param pt
     * @return
     */
    @Override
    public boolean generateRedwoodTree(EditSession editSession, Vector pt) {
        return world.generateTree(BukkitUtil.toLocation(world, pt), TreeType.REDWOOD,
                new EditSessionBlockChangeDelegate(editSession));
    }

    /**
     * Generate a redwood tree at a location.
     * 
     * @param pt
     * @return
     */
    @Override
    public boolean generateTallRedwoodTree(EditSession editSession, Vector pt) {
        return world.generateTree(BukkitUtil.toLocation(world, pt), TreeType.TALL_REDWOOD,
                new EditSessionBlockChangeDelegate(editSession));
    }

    /**
     * Drop an item.
     *
     * @param pt
     * @param item
     */
    @Override
    public void dropItem(Vector pt, BaseItemStack item) {
        ItemStack bukkitItem = new ItemStack(item.getType(), item.getAmount(),
                (byte) item.getDamage());
        world.dropItemNaturally(toLocation(pt), bukkitItem);

    }

    /**
     * Kill mobs in an area, excluding tamed wolves.
     * 
     * @param origin
     * @param radius -1 for all mobs
     * @return
     */
    @Override
    public int killMobs(Vector origin, int radius) {
        return killMobs(origin, radius, false);
    }

    /**
     * Kill mobs in an area.
     * 
     * @param origin
     * @param radius -1 for all mobs
     * @param killPets true to kill tames wolves
     * @return
     */
    @Override
    public int killMobs(Vector origin, int radius, boolean killPets) {
        int num = 0;
        double radiusSq = Math.pow(radius, 2);

        for (LivingEntity ent : world.getLivingEntities()) {
            if (!killPets && ent instanceof Tameable && ((Tameable) ent).isTamed()) {
                continue; // tamed wolf
            }
            if (ent instanceof LivingEntity && !(ent instanceof HumanEntity)) {
                if (radius == -1
                        || origin.distanceSq(BukkitUtil.toVector(ent.getLocation())) <= radiusSq) {
                    ent.remove();
                    ++num;
                }
            }
        }

        return num;
    }

    /**
     * Remove entities in an area.
     * 
     * @param origin
     * @param radius
     * @return
     */
    @Override
    public int removeEntities(EntityType type, Vector origin, int radius) {
        int num = 0;
        double radiusSq = Math.pow(radius, 2);

        for (Entity ent : world.getEntities()) {
            if (radius != -1
                    && origin.distanceSq(BukkitUtil.toVector(ent.getLocation())) > radiusSq) {
                continue;
            }

            switch (type) {
            case ARROWS:
                if (ent instanceof Arrow) {
                    ent.remove();
                    ++num;
                }
                break;

            case BOATS:
                if (ent instanceof Boat) {
                    ent.remove();
                    ++num;
                }
                break;

            case ITEMS:
                if (ent instanceof Item) {
                    ent.remove();
                    ++num;
                }
                break;

            case MINECARTS:
                if (ent instanceof Minecart) {
                    ent.remove();
                    ++num;
                }
                break;

            case PAINTINGS:
                if (ent instanceof Painting) {
                    ent.remove();
                    ++num;
                }
                break;

            case TNT:
                if (ent instanceof TNTPrimed) {
                    ent.remove();
                    ++num;
                }
                break;

            case XP_ORBS:
                if (ent instanceof ExperienceOrb) {
                    ent.remove();
                    ++num;
                }
                break;
            }
        }

        return num;
    }

    private Location toLocation(Vector pt) {
        return new Location(world, pt.getX(), pt.getY(), pt.getZ());
    }

    /**
     * Set a sign's text.
     * 
     * @param pt
     * @param text
     * @return
     */
    private boolean setSignText(Vector pt, String[] text) {
        Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (block == null) return false;
        BlockState state = block.getState();
        if (state == null || !(state instanceof Sign)) return false;
        Sign sign = (Sign) state;
        sign.setLine(0, text[0]);
        sign.setLine(1, text[1]);
        sign.setLine(2, text[2]);
        sign.setLine(3, text[3]);
        sign.update();
        return true;
    }

    /**
     * Get a sign's text.
     * 
     * @param pt
     * @return
     */
    private String[] getSignText(Vector pt) {
        Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (block == null) return new String[] { "", "", "", "" };
        BlockState state = block.getState();
        if (state == null || !(state instanceof Sign)) return new String[] { "", "", "", "" };
        Sign sign = (Sign) state;
        String line0 = sign.getLine(0);
        String line1 = sign.getLine(1);
        String line2 = sign.getLine(2);
        String line3 = sign.getLine(3);
        return new String[] {
                line0 != null ? line0 : "",
                line1 != null ? line1 : "",
                line2 != null ? line2 : "",
                line3 != null ? line3 : "",
            };
    }

    /**
     * Get a container block's contents.
     * 
     * @param pt
     * @return
     */
    private BaseItemStack[] getContainerBlockContents(Vector pt) {
        Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (block == null) {
            return new BaseItemStack[0];
        }
        BlockState state = block.getState();
        if (!(state instanceof org.bukkit.block.ContainerBlock)) {
            return new BaseItemStack[0];
        }

        org.bukkit.block.ContainerBlock container = (org.bukkit.block.ContainerBlock) state;
        Inventory inven = container.getInventory();
        int size = inven.getSize();
        BaseItemStack[] contents = new BaseItemStack[size];

        for (int i = 0; i < size; ++i) {
            ItemStack bukkitStack = inven.getItem(i);
            if (bukkitStack.getTypeId() > 0) {
                contents[i] = new BaseItemStack(
                        bukkitStack.getTypeId(),
                        bukkitStack.getAmount(),
                        bukkitStack.getDurability());
            }
        }

        return contents;
    }

    /**
     * Set a container block's contents.
     * 
     * @param pt
     * @param contents
     * @return
     */
    private boolean setContainerBlockContents(Vector pt, BaseItemStack[] contents) {
        Block block = world.getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (block == null) {
            return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof org.bukkit.block.ContainerBlock)) {
            return false;
        }

        org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
        Inventory inven = chest.getInventory();
        int size = inven.getSize();

        for (int i = 0; i < size; ++i) {
            if (i >= contents.length) {
                break;
            }

            if (contents[i] != null) {
                inven.setItem(i, new ItemStack(contents[i].getType(),
                        contents[i].getAmount(),
                        (byte) contents[i].getDamage()));
            } else {
                inven.setItem(i, null);
            }
        }

        return true;
    }

    /**
     * Returns whether a block has a valid ID.
     * 
     * @param type
     * @return
     */
    @Override
    public boolean isValidBlockType(int type) {
        return type <= 255 && Material.getMaterial(type) != null;
    }

    @Override
    public void checkLoadedChunk(Vector pt) {
        if (!world.isChunkLoaded(pt.getBlockX() >> 4, pt.getBlockZ() >> 4)) {
            world.loadChunk(pt.getBlockX() >> 4, pt.getBlockZ() >> 4);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BukkitWorld)) {
            return false;
        }

        return ((BukkitWorld) other).world.equals(world);
    }

    @Override
    public int hashCode() {
        return world.hashCode();
    }

    @Override
    public int getHeight() {
        return world.getMaxHeight() - 1;
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
        fixLighting(chunks);

        for (BlockVector2D chunkPos : chunks) {
            world.refreshChunk(chunkPos.getBlockX(), chunkPos.getBlockZ());
        }
    }

    private static final int chunkSizeX = 16;
    private static final int chunkSizeY = 128;
    private static final int chunkSizeZ = 16;

    @Override
    public void fixLighting(Iterable<BlockVector2D> chunks) {
        if (!fastLightingAvailable) {
            return;
        }

        try {
            Object notchWorld = CraftWorld_getHandle.invoke(world);
            for (BlockVector2D chunkPos : chunks) {
                final int chunkX = chunkPos.getBlockX();
                final int chunkZ = chunkPos.getBlockZ();

                final Object notchChunk = World_getChunkFromChunkCoords.invoke(notchWorld, chunkX, chunkZ);

                // Fix skylight
                final byte[] blocks = (byte[]) Chunk_blocks.get(notchChunk);
                final int length = blocks.length;
                Chunk_skylightMap.set(notchChunk, NibbleArray_ctor.newInstance(length, 7));

                Chunk_generateSkylightMap.invoke(notchChunk);

                // Fix blocklight
                Chunk_blocklightMap.set(notchChunk, NibbleArray_ctor.newInstance(length, 7));

                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                List<BlockState> lightEmitters = new ArrayList<BlockState>();

                for (int x = 0; x < chunkSizeX; ++x) {
                    boolean xBorder = x == 0 || x == chunkSizeX - 1;
                    for (int z = 0; z < chunkSizeZ; ++z) {
                        boolean zBorder = z == 0 || z == chunkSizeZ - 1;
                        for (int y = 0; y < chunkSizeY; ++y) {
                            final int index = y + z * chunkSizeY + x * chunkSizeY * chunkSizeZ;
                            byte blockID = blocks[index];
                            if (!BlockType.emitsLight(blockID)) {
                                if (xBorder || zBorder && BlockType.isTranslucent(blockID)) {
                                    lightEmitters.add(chunk.getBlock(x, y, z).getState());
                                    if (blockID == 20) {
                                        blocks[index] = 0;
                                    } else {
                                        blocks[index] = 20;
                                    }

                                }
                                continue;
                            }

                            lightEmitters.add(chunk.getBlock(x, y, z).getState());

                            blocks[index] = 0;
                        }
                    }
                }

                for (BlockState lightEmitter : lightEmitters) {
                    lightEmitter.update(true);
                }
            }
        } catch (Exception e) {
            System.out.println("Fast Mode: Could not fix lighting. Probably an incompatible version of CraftBukkit.");
            e.printStackTrace();
        }
    }

    private static boolean fastLightingAvailable = false;
    private static int[] Block_lightOpacity;
    private static Method CraftWorld_getHandle;
    private static Method World_getChunkFromChunkCoords;
    private static Method Chunk_generateSkylightMap;
    private static Method Chunk_relightBlock;
    private static Field Chunk_blocks;
    private static Field Chunk_skylightMap;
    private static Field Chunk_blocklightMap;
    private static Constructor<?> NibbleArray_ctor;
    static {
        if (Bukkit.getServer().getName().equalsIgnoreCase("CraftBukkit")) {
            try {
                Block_lightOpacity = (int[]) Class.forName("net.minecraft.server.Block").getDeclaredField("q").get(null);

                CraftWorld_getHandle = Class.forName("org.bukkit.craftbukkit.CraftWorld").getMethod("getHandle");

                World_getChunkFromChunkCoords = Class.forName("net.minecraft.server.World").getMethod("getChunkAt", int.class, int.class);
                Chunk_generateSkylightMap = Class.forName("net.minecraft.server.Chunk").getMethod("initLighting");
                Chunk_relightBlock = Class.forName("net.minecraft.server.Chunk").getDeclaredMethod("g", int.class, int.class, int.class);
                Chunk_relightBlock.setAccessible(true);

                Chunk_blocks = Class.forName("net.minecraft.server.Chunk").getField("b");
                Chunk_skylightMap = Class.forName("net.minecraft.server.Chunk").getField("h");
                Chunk_blocklightMap = Class.forName("net.minecraft.server.Chunk").getField("i");
                NibbleArray_ctor = Class.forName("net.minecraft.server.NibbleArray").getConstructor(int.class, int.class);

                //fastLightingAvailable = true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
