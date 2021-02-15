package com.zundrel.conveyance.common.blocks.entities;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorConveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.conveyors.ConveyorProperties;
import com.zundrel.conveyance.common.registries.ConveyanceBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VerticalConveyorBlockEntity extends ConveyorBlockEntity {
    protected boolean up = false;
    protected int horizontalPosition;
    protected int prevHorizontalPosition;

    public VerticalConveyorBlockEntity() {
        super(ConveyanceBlockEntities.VERTICAL_CONVEYOR);
    }

    public VerticalConveyorBlockEntity(BlockEntityType type) {
        super(type);
    }

    @Override
    public void tick() {
        Direction direction = getCachedState().get(Properties.FACING);

        //int speed = ((Conveyor) getCachedState().getBlock()).getSpeed();
        int speed = 16;

        if (!isEmpty()) {
            if (getCachedState().get(ConveyorProperties.CONVEYOR)) {
                BlockPos conveyorPos = getPos().offset(direction).up();
                if (getWorld().getBlockEntity(conveyorPos) instanceof Conveyable) {
                    Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(conveyorPos);
                    if (this.position < speed) {
                        handleMovement(conveyable, speed, false);
                    } else {
                        this.prevPosition = speed;
                        handleMovementHorizontal(conveyable, speed, true);
                    }
                }
            } else if (this.up) {
                BlockPos upPos = getPos().up();
                if (getWorld().getBlockEntity(upPos) instanceof Conveyable) {
                    Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(upPos);
                    this.handleMovement(conveyable, speed, true);
                }
            } else {
                this.setPosition(0);
            }
        } else {
            this.setPosition(0);
        }
    }

    public void handleMovementHorizontal(Conveyable conveyable, int speed, boolean transition) {
        if (conveyable.accepts(getStack())) {
            if (this.horizontalPosition < speed) {
                setHorizontalPosition(getHorizontalPosition() + 2);
            } else if (transition && !getWorld().isClient() && this.horizontalPosition >= speed) {
                conveyable.give(getStack());
                removeStack();
            }
        } else if (conveyable instanceof ConveyorConveyable) {
            ConveyorConveyable conveyor = (ConveyorConveyable) conveyable;

            if (this.horizontalPosition < speed && this.horizontalPosition + 4 < conveyor.getPosition() && conveyor.getPosition() > 4) {
                setHorizontalPosition(getHorizontalPosition() + 2);
            } else {
                this.prevHorizontalPosition = this.horizontalPosition;
            }
        }
    }

    @Override
    public boolean validInputSide(Direction direction) {

        return !getCachedState().get(ConveyorProperties.FRONT) && direction == Direction.DOWN ||
                direction == getCachedState().get(Properties.FACING).getOpposite();
    }

    @Override
    public boolean isOutputSide(Direction direction, ConveyorType type) {
        return type == ConveyorType.NORMAL ? getCachedState().get(Properties.FACING) == direction : direction == Direction.UP;
    }

    @Override
    public ItemStack removeStack() {
        this.horizontalPosition = 0;
        this.prevHorizontalPosition = 0;
        return super.removeStack();
    }

    public boolean hasUp() {
        return this.up;
    }

    public void setUp(boolean up) {
        this.up = up;
        markDirty();
    }

    @Override
    public int[] getRenderAttachmentData() {
        return new int[]{
                this.position,
                this.prevPosition,
                this.horizontalPosition,
                this.prevHorizontalPosition
        };
    }

    public int getHorizontalPosition() {
        return this.horizontalPosition;
    }

    public void setHorizontalPosition(int horizontalPosition) {
        if (horizontalPosition == 0)
            this.prevHorizontalPosition = 0;
        else
            this.prevHorizontalPosition = this.horizontalPosition;

        this.horizontalPosition = horizontalPosition;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag compoundTag) {
        super.fromTag(state, compoundTag);
        this.up = compoundTag.getBoolean("up");
        this.horizontalPosition = compoundTag.getInt("horizontalPosition");
        this.prevHorizontalPosition = this.horizontalPosition = compoundTag.getInt("horizontalPosition");
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        fromTag(getCachedState(), compoundTag);
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        compoundTag.putBoolean("up", this.up);
        compoundTag.putInt("horizontalPosition", this.horizontalPosition);
        return super.toTag(compoundTag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return toTag(compoundTag);
    }
}
