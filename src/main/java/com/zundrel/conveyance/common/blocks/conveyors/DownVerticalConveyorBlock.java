package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.entities.ConveyorBlockEntity;
import com.zundrel.conveyance.common.blocks.entities.DownVerticalConveyorBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class DownVerticalConveyorBlock extends VerticalConveyorBlock {
    public DownVerticalConveyorBlock(Settings settings, int speed) {
        super(settings, speed);

        // FRONT    : The exit-point to a horizontal conveyor
        // CONVEYOR : An attached horizontal conveyour for input (top)

        setDefaultState(getDefaultState()
                .with(ConveyorProperties.FRONT, false)
                .with(ConveyorProperties.CONVEYOR, false)
        );
    }

    @Override
    public ConveyorType getType() {
        return ConveyorType.DOWN_VERTICAL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new DownVerticalConveyorBlockEntity();
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState originalState, Direction direction, BlockState newState, WorldAccess world, BlockPos blockPos, BlockPos posFrom) {
        if (newState.isAir()) {
            return originalState;
        }

        direction = originalState.get(Properties.FACING);

        BlockPos frontPos = blockPos.offset(direction.getOpposite());
        BlockEntity frontBlockEntity = world.getBlockEntity(frontPos);
        if (frontBlockEntity instanceof Conveyable && ((Conveyable) frontBlockEntity).validInputSide(direction)) {
            originalState = originalState.with(ConveyorProperties.FRONT, true);
        } else {
            originalState = originalState.with(ConveyorProperties.FRONT, false);
        }

        BlockPos conveyorPos = blockPos.offset(direction).up();
        BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
        if (world.isAir(blockPos.up()) &&
                conveyorBlockEntity instanceof Conveyable &&
                !((Conveyable) conveyorBlockEntity).hasBeenRemoved() &&
                ((Conveyable) conveyorBlockEntity).isOutputSide(direction.getOpposite(), getType()))

            originalState = originalState.with(ConveyorProperties.CONVEYOR, true);
        else
            originalState = originalState.with(ConveyorProperties.CONVEYOR, false);

        return originalState;
    }

    @Override
    public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
        setInputs(blockState, world, blockPos);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        setInputs(state, world, pos);
        //checkForConveyor(world, state, world.getBlockEntity(pos), state.get(Properties.FACING), pos, pos.up());
    }

    /**
     * Sets the proper inputs
     *
     * @param blockState Blockstate of the conveyor
     * @param world      World-reference
     * @param blockPos   Block position-reference
     */
    @Override
    public void setInputs(BlockState blockState, World world, BlockPos blockPos) {
        Direction direction = blockState.get(Properties.FACING);
        ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

        // Check below
        BlockPos downPos = blockPos.down(1);
        BlockEntity downBlockEntity = world.getBlockEntity(downPos);
        if (downBlockEntity instanceof Conveyable && ((Conveyable) downBlockEntity).validInputSide(Direction.UP)) {
            blockEntity.setDown(true);
        } else {
            blockEntity.setDown(false);
        }

        BlockPos conveyorPos = blockPos.offset(direction).up();
        BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
        checkForConveyor(world, blockState, conveyorBlockEntity, direction, blockPos, blockPos.up());

    }

    @Override
    public void checkForConveyor(World world, BlockState blockState, BlockEntity conveyorBlockEntity, Direction direction, BlockPos pos, BlockPos upPos) {
        BlockState newState = blockState;


        // Front == exit!
        BlockPos frontPos = pos.offset(direction.getOpposite());
        BlockEntity frontBlockEntity = world.getBlockEntity(frontPos);
        if (frontBlockEntity instanceof Conveyable && ((Conveyable) frontBlockEntity).validInputSide(direction)) {
            // We have a front-exit
            newState = newState.with(ConveyorProperties.FRONT, true);
        } else {
            newState = newState.with(ConveyorProperties.FRONT, false);
        }

        // Conveyor == input
        if (world.isAir(upPos) &&
                conveyorBlockEntity instanceof Conveyable
                && !((Conveyable) conveyorBlockEntity).hasBeenRemoved()
                && ((Conveyable) conveyorBlockEntity).isOutputSide(direction.getOpposite(), getType())) {
            // We have a conveyor-input
            newState = newState.with(ConveyorProperties.CONVEYOR, true);
        } else {
            newState = newState.with(ConveyorProperties.CONVEYOR, false);
        }
        world.setBlockState(pos, newState, 8);
    }
}
