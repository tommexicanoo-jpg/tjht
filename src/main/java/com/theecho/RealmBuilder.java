package com.theecho;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Construye la réplica oscura y subterránea de tu base dentro de la dimensión the_echo:mirror_realm.
 * Copia los bloques alrededor de tu cama, los corrompe y los encierra en una caja de deepslate.
 */
public final class RealmBuilder {
    private static final int WALL = 3, DOWN = 6, UP = 20, FLOOR_BOTTOM = 4;

    private RealmBuilder() {}

    public static BlockPos build(ServerWorld src, ServerWorld dst, BlockPos srcOrigin, BlockPos dstOrigin, Random rand) {
        final int R = EchoConfig.INSTANCE.realmRadius;
        final int dy = dstOrigin.getY() - srcOrigin.getY();
        final int minY = dstOrigin.getY() - DOWN;
        final int maxY = dstOrigin.getY() + UP;
        final int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

        BlockState[] deep = {
                Blocks.DEEPSLATE.getDefaultState(), Blocks.DEEPSLATE.getDefaultState(),
                Blocks.COBBLED_DEEPSLATE.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState()
        };
        BlockState air = Blocks.AIR.getDefaultState();
        BlockPos.Mutable dp = new BlockPos.Mutable();
        BlockPos.Mutable sp = new BlockPos.Mutable();
        List<BlockPos> signs = new ArrayList<>();

        for (int dx = -(R + WALL); dx <= R + WALL; dx++) {
            for (int dz = -(R + WALL); dz <= R + WALL; dz++) {
                boolean inside = Math.abs(dx) <= R && Math.abs(dz) <= R;
                int x = dstOrigin.getX() + dx, z = dstOrigin.getZ() + dz;
                int sx = srcOrigin.getX() + dx, sz = srcOrigin.getZ() + dz;

                if (!inside) { // muros
                    for (int y = FLOOR_BOTTOM; y <= maxY + WALL; y++) {
                        put(dst, dp.set(x, y, z), deep[rand.nextInt(deep.length)], flags);
                    }
                    continue;
                }
                for (int y = FLOOR_BOTTOM; y < minY; y++) { // suelo
                    put(dst, dp.set(x, y, z), deep[rand.nextInt(deep.length)], flags);
                }
                for (int y = minY; y <= maxY; y++) { // copia corrupta
                    int srcY = y - dy;
                    BlockState out;
                    if (src.isOutOfHeightLimit(srcY)) {
                        out = air;
                    } else {
                        sp.set(sx, srcY, sz);
                        out = corrupt(src.getBlockState(sp), src, sp, rand);
                    }
                    put(dst, dp.set(x, y, z), out, flags);
                    if (out.getBlock() instanceof AbstractSignBlock) signs.add(dp.toImmutable());
                }
                for (int y = maxY + 1; y <= maxY + WALL; y++) { // techo
                    put(dst, dp.set(x, y, z), deep[rand.nextInt(deep.length)], flags);
                }
            }
        }

        // Punto de llegada despejado y con suelo
        dst.setBlockState(dstOrigin, air, Block.NOTIFY_ALL);
        dst.setBlockState(dstOrigin.up(), air, Block.NOTIFY_ALL);
        if (!dst.getBlockState(dstOrigin.down()).isSolidBlock(dst, dstOrigin.down())) {
            dst.setBlockState(dstOrigin.down(), Blocks.DEEPSLATE.getDefaultState(), Block.NOTIFY_ALL);
        }

        // Carteles con un mensaje
        for (BlockPos pos : signs) {
            BlockEntity be = dst.getBlockEntity(pos);
            if (be instanceof SignBlockEntity sign) {
                sign.changeText(t -> t.withMessage(1, Text.literal("NO MIRES"))
                        .withMessage(2, Text.literal("ATRÁS")), true);
            }
        }

        // El Original
        BlockPos orig = randomStandable(dst, dstOrigin, Math.max(6, R - 8), Math.max(7, R - 2), rand);
        if (orig == null) {
            orig = dstOrigin.add(Math.max(4, R - 3), 0, 0);
            dst.setBlockState(orig, air, Block.NOTIFY_ALL);
            dst.setBlockState(orig.up(), air, Block.NOTIFY_ALL);
            dst.setBlockState(orig.down(), Blocks.DEEPSLATE.getDefaultState(), Block.NOTIFY_ALL);
        }
        dst.setBlockState(orig, ModBlocks.ECHO_ORIGINAL.getDefaultState(), Block.NOTIFY_ALL);
        return orig;
    }

    /** Solo escribe si el bloque cambia: evita miles de escrituras inútiles de aire sobre aire. */
    private static void put(ServerWorld w, BlockPos pos, BlockState state, int flags) {
        BlockState cur = w.getBlockState(pos);
        if (cur == state) return;
        w.setBlockState(pos, state, flags);
    }

    private static BlockState corrupt(BlockState s, ServerWorld src, BlockPos srcPos, Random r) {
        BlockState air = Blocks.AIR.getDefaultState();
        Block b = s.getBlock();
        if (s.isAir()) return r.nextInt(900) == 0 ? Blocks.COBWEB.getDefaultState() : s;
        if (b instanceof FluidBlock || s.getLuminance() > 0 || s.isIn(BlockTags.BEDS)) return air;
        if (!s.getFluidState().isEmpty()) s = s.withIfExists(Properties.WATERLOGGED, false);
        if (b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == Blocks.PODZOL) {
            return (r.nextInt(6) == 0 ? Blocks.MOSS_BLOCK : Blocks.COARSE_DIRT).getDefaultState();
        }
        if (s.isIn(BlockTags.CROPS)) return air;
        if (s.isReplaceable()) return r.nextInt(8) == 0 ? Blocks.COBWEB.getDefaultState() : air;
        if (s.isIn(BlockTags.LEAVES)) return s.withIfExists(LeavesBlock.PERSISTENT, true);
        if (s.isFullCube(src, srcPos) && r.nextInt(40) == 0) return air; // bloques que "faltan"
        return s;
    }

    /** Busca una celda de aire con suelo sólido a distancia [minR, maxR] del centro. */
    public static BlockPos randomStandable(ServerWorld w, BlockPos center, int minR, int maxR, Random rand) {
        for (int i = 0; i < 300; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double dist = minR + rand.nextDouble() * Math.max(1, maxR - minR);
            int x = (int) Math.floor(center.getX() + Math.cos(ang) * dist);
            int z = (int) Math.floor(center.getZ() + Math.sin(ang) * dist);
            for (int y = center.getY() + 8; y >= center.getY() - 4; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (w.getBlockState(p).isAir() && w.getBlockState(p.up()).isAir()
                        && w.getBlockState(p.down()).isSolidBlock(w, p.down())
                        && w.getFluidState(p.down()).isEmpty()) {
                    return p;
                }
            }
        }
        return null;
    }
}
