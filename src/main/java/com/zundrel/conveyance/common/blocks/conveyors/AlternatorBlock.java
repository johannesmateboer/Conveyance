package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.ConveyableBlock;
import com.zundrel.conveyance.common.blocks.entities.AlternatorBlockEntity;
import com.zundrel.conveyance.common.blocks.entities.DoubleMachineBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class AlternatorBlock extends BlockWithEntity implements BlockEntityProvider, ConveyableBlock {
    public AlternatorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new AlternatorBlockEntity();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManagerBuilder) {
        stateManagerBuilder.add(Properties.FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext) {
        return this.getDefaultState().with(Properties.FACING, itemPlacementContext.getPlayer().isSneaking() ? itemPlacementContext.getPlayerFacing().getOpposite() : itemPlacementContext.getPlayerFacing());
    }

    @Override
    public void onBlockAdded(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {
        updateDiagonals(world, this, blockPos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean notify) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DoubleMachineBlockEntity) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ((DoubleMachineBlockEntity) blockEntity).getLeftStack());
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ((DoubleMachineBlockEntity) blockEntity).getRightStack());
                ((DoubleMachineBlockEntity) blockEntity).setRemoved(true);
            }

            super.onStateReplaced(state, world, pos, newState, notify);
        }

        updateDiagonals(world, this, pos);
    }

    @Override
    public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
        setInputs(blockState, world, blockPos);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        setInputs(state, world, pos);
    }

    /**
     * Sets the proper inputs
     *
     * @param blockState Blockstate of the conveyor
     * @param world      World-reference
     * @param blockPos   Block position-reference
     */
    private void setInputs(BlockState blockState, World world, BlockPos blockPos) {
        Direction direction = blockState.get(Properties.FACING);
        DoubleMachineBlockEntity machineBlockEntity = (DoubleMachineBlockEntity) world.getBlockEntity(blockPos);

        BlockPos leftPos = blockPos.offset(direction.rotateYCounterclockwise());
        BlockPos rightPos = blockPos.offset(direction.rotateYClockwise());

        BlockEntity leftBlockEntity = world.getBlockEntity(leftPos);
        if (leftBlockEntity instanceof Conveyable && ((Conveyable) leftBlockEntity).validInputSide(direction.rotateYClockwise()))
            machineBlockEntity.setLeft(true);
        else
            machineBlockEntity.setLeft(false);

        BlockEntity rightBlockEntity = world.getBlockEntity(rightPos);
        if (rightBlockEntity instanceof Conveyable && ((Conveyable) rightBlockEntity).validInputSide(direction.rotateYCounterclockwise()))
            machineBlockEntity.setRight(true);
        else
            machineBlockEntity.setRight(false);
    }
}
