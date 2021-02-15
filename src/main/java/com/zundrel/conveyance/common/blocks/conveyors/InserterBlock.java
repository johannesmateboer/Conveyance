package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.ConveyableBlock;
import com.zundrel.conveyance.common.blocks.entities.InserterBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class InserterBlock extends BlockWithEntity implements BlockEntityProvider, ConveyableBlock {
    private String type;
    private int speed;

    public InserterBlock(String type, int speed, Settings settings) {
        super(settings);

        this.type = type;
        this.speed = speed;
    }

    public String getType() {
        return type;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new InserterBlockEntity();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManagerBuilder) {
        stateManagerBuilder.add(Properties.FACING);

    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext) {
        return this.getDefaultState().with(Properties.FACING, itemPlacementContext.getPlayer().isSneaking() ? itemPlacementContext.getPlayerFacing().getOpposite() : itemPlacementContext.getPlayerFacing());
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean notify) {
        if (state.getBlock() != newState.getBlock()) {

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof InserterBlockEntity) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ((InserterBlockEntity) blockEntity).getStack());
            }

            super.onStateReplaced(state, world, pos, newState, notify);
        }

        updateDiagonals(world, this, pos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 1, 0.5, 1);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState originalState, Direction direction, BlockState newState, WorldAccess world, BlockPos blockPos, BlockPos posFrom) {
        neighborUpdate(originalState, (World) world, blockPos, newState.getBlock(), posFrom, false);
        return originalState;
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
        InserterBlockEntity machineBlockEntity = (InserterBlockEntity) world.getBlockEntity(blockPos);

        BlockPos frontPos = blockPos.offset(direction);
        BlockPos behindPos = blockPos.offset(direction.getOpposite());

        BlockEntity frontBlockEntity = world.getBlockEntity(frontPos);
        if (frontBlockEntity instanceof Inventory && !(frontBlockEntity instanceof InserterBlockEntity)) {
            machineBlockEntity.setHasOutput(true);
        } else {
            machineBlockEntity.setHasOutput(false);
        }

        BlockEntity behindBlockEntity = world.getBlockEntity(behindPos);
        if (behindBlockEntity instanceof Inventory && !(frontBlockEntity instanceof InserterBlockEntity)) {
            machineBlockEntity.setHasInput(true);
        } else {
            machineBlockEntity.setHasInput(false);
        }
    }

    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos blockPos, PlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult) {
        InserterBlockEntity inserter = (InserterBlockEntity) world.getBlockEntity(blockPos);
        playerEntity.playSound(SoundEvents.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
        if (playerEntity.getStackInHand(hand).isEmpty()) {
            // Player is sneaking, no item, reset filter
            inserter.clearFilterItem();
            return ActionResult.SUCCESS;
        } else {
            inserter.setFilterItem(playerEntity.getStackInHand(hand).getItem());
            world.playSound(playerEntity, blockPos, SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        }
    }
}
