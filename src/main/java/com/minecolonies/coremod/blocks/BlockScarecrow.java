package com.minecolonies.coremod.blocks;

import com.minecolonies.api.blocks.huts.AbstractBlockMinecoloniesDefault;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.coremod.tileentities.ScarecrowTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.minecolonies.api.util.constant.Suppression.DEPRECATION;
import static net.minecraft.util.Direction.NORTH;
import static net.minecraft.util.Direction.fromAngle;

/**
 * The class handling the fieldBlocks, placement and activation.
 */
public class BlockScarecrow extends AbstractBlockMinecoloniesDefault<BlockScarecrow>
{

    /**
     * Constructor called on block placement.
     */
    public BlockScarecrow()
    {
        super(Properties.create(Material.WOOD).hardnessAndResistance(HARDNESS, RESISTANCE));
        setRegistryName(REGISTRY_NAME);
        this.setDefaultState(this.getDefaultState().with(FACING, NORTH));

    }

    @NotNull
    @Override
    @SuppressWarnings(DEPRECATION)
    public BlockRenderType getRenderType(final BlockState state)
    {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
      final BlockState state, final IBlockReader worldIn, final BlockPos pos, final ISelectionContext context)
    {
        return Block.makeCuboidShape((float) START_COLLISION,
          (float) BOTTOM_COLLISION,
          (float) START_COLLISION,
          (float) END_COLLISION,
          (float) HEIGHT_COLLISION,
          (float) END_COLLISION);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world)
    {
        return new ScarecrowTileEntity();
    }

    @Override
    public boolean doesSideBlockRendering(final BlockState state, final IEnviromentBlockReader world, final BlockPos pos, final Direction face)
    {
        return false;
    }

    @NotNull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean onBlockActivated(final BlockState state, final World worldIn, final BlockPos pos, final PlayerEntity player, final Hand handIn, final BlockRayTraceResult hit)
    {
        //If the world is server, open the inventory of the field.
        if (!worldIn.isRemote)
        {
            final TileEntity entity = worldIn.getTileEntity(pos);
            if (entity instanceof ScarecrowTileEntity)
            {
                player.openContainer((ScarecrowTileEntity) entity);
                return true;
            }
        }
        return false;
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context)
    {
        @NotNull final Direction Direction = (context.getPlayer() == null) ? NORTH : fromAngle(context.getPlayer().rotationYaw);
        return this.getDefaultState().with(FACING, Direction);
    }

    @Override
    public void onBlockPlacedBy(final World worldIn, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack)
    {
        //Only work on server side.
        if (worldIn.isRemote)
        {
            return;
        }

        if (placer instanceof PlayerEntity)
        {
            @Nullable final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(worldIn, pos);

            if (colony != null)
            {
                final ScarecrowTileEntity scareCrow = (ScarecrowTileEntity) worldIn.getTileEntity(pos);
                if (scareCrow != null)
                {
                    colony.getBuildingManager().addNewField(scareCrow, pos, worldIn);
                }
            }
        }
    }

    @Override
    public void onExplosionDestroy(final World worldIn, final BlockPos pos, final Explosion explosionIn)
    {
        notifyColonyAboutDestruction(worldIn, pos);
        super.onExplosionDestroy(worldIn, pos, explosionIn);
    }

    @Override
    public void onBlockHarvested(final World worldIn, @NotNull final BlockPos pos, final BlockState state, @NotNull final PlayerEntity player)
    {
        notifyColonyAboutDestruction(worldIn, pos);
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public void onPlayerDestroy(final IWorld worldIn, final BlockPos pos, final BlockState state)
    {
        notifyColonyAboutDestruction(worldIn, pos);
        super.onPlayerDestroy(worldIn, pos, state);
    }

    /**
     * Notify the colony about the destruction of the field.
     * @param worldIn the world.
     * @param pos the position.
     */
    private static void notifyColonyAboutDestruction(final IWorld worldIn, final BlockPos pos)
    {
        if (!worldIn.isRemote())
        {
            @Nullable final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld((World) worldIn, pos);
            if (colony != null)
            {
                colony.getBuildingManager().removeField(pos);
            }
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean hasTileEntity(final BlockState state)
    {
        return true;
    }
}