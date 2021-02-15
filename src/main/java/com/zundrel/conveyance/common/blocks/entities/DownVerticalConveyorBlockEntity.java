package com.zundrel.conveyance.common.blocks.entities;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.conveyors.ConveyorProperties;
import com.zundrel.conveyance.common.registries.ConveyanceBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class DownVerticalConveyorBlockEntity extends ConveyorBlockEntity {
    protected boolean down = false;
    protected int horizontalPosition;
    protected int prevHorizontalPosition;

    public DownVerticalConveyorBlockEntity() {
        super(ConveyanceBlockEntities.DOWN_VERTICAL_CONVEYOR);
    }

    @Override
    public void tick() {
        Direction direction = getCachedState().get(Properties.FACING);
        int speed = ((Conveyor) getCachedState().getBlock()).getSpeed();

        if (!this.isEmpty()) {

            if (this.hasConveyorBelow(getPos())) {

                // Move to DownVertical below self
                this.handleMovement(this.getDownConveyor(getPos()), speed, true);

            } else if (this.hasConveyorInFront(getPos())) {

                // Front to normal conveyor - exit
                if (this.getPosition() < speed) {
                    this.handleMovementHorizontal(this.getConveyorInFront(getPos()), speed, false);
                } else {
                    this.prevPosition = speed;
                    this.handleMovementHorizontal(this.getConveyorInFront(getPos()), speed, true);
                }

            } else if (getCachedState().get(ConveyorProperties.CONVEYOR)) {
                // Input from normal conveyor
                BlockPos inputConveyorPos = getPos().offset(direction).up();
                if (getWorld().getBlockEntity(inputConveyorPos) instanceof Conveyable) {
                    Conveyable inputConveyor = (Conveyable) getWorld().getBlockEntity(inputConveyorPos);
                    if (this.getPosition() < speed) {
                        handleMovementHorizontal(inputConveyor, speed, false);
                    } else {
                        this.prevPosition = speed;
                        handleMovementHorizontal(inputConveyor, speed, true);
                    }
                }
            } else {
                setPosition(0);
            }
        } else {
            setPosition(0);
        }
    }

    public void handleMovementHorizontal(Conveyable conveyable, int speed, boolean transition) {
        // Is the stack already at exit-point?
        if (this.getHorizontalPosition() < speed) {
            this.setHorizontalPosition(getHorizontalPosition() + 1);
            return;
        }
        if (conveyable.accepts(getStack())) {
            conveyable.give(getStack());
            this.removeStack();
        }
    }

    @Override
    public void handleMovement(Conveyable conveyable, int speed, boolean transition) {
        // Is the stack already at exit-point?
        if (this.getPosition() < speed) {
            this.setPosition(this.getPosition() + 1);
            return;
        }

        // Give the stack to the receiver (the lower conveyor-belt)
        if (conveyable.accepts(this.getStack())) {
            conveyable.give(this.getStack());
            this.removeStack();
        }
    }

    @Override
    public boolean validInputSide(Direction direction) {
        return direction == Direction.UP || direction == getCachedState().get(Properties.FACING);
    }

    @Override
    public boolean isOutputSide(Direction direction, ConveyorType type) {
        return type == ConveyorType.NORMAL ? getCachedState().get(Properties.FACING).getOpposite() == direction : direction == Direction.DOWN;
    }

    @Override
    public ItemStack removeStack() {
        this.setPosition(0);
        this.horizontalPosition = 0;
        this.prevHorizontalPosition = 0;
        return super.removeStack();
    }

    /**
     * Checks if the block below is a conveyor
     *
     * @param blockPosition Position of Self
     * @return boolean If there is a conveyor below
     */
    public boolean hasConveyorBelow(BlockPos blockPosition) {
        BlockPos downPosition = blockPosition.down();
        return (getWorld().getBlockEntity(downPosition) instanceof Conveyable);
    }

    public boolean hasConveyorInFront(BlockPos blockPosition) {
        Direction direction = getCachedState().get(Properties.FACING);
        BlockPos outputPosition = blockPosition.offset(direction.getOpposite());
        return (getWorld().getBlockEntity(outputPosition) instanceof Conveyable);
    }

    /**
     * Gets the conveyor below self
     *
     * @param blockPosition The position of Self
     * @return Conveyable The Conveyable below the position of Self
     */
    private Conveyable getDownConveyor(BlockPos blockPosition) {
        BlockPos downPosition = blockPosition.down();
        Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(downPosition);
        return conveyable;
    }

    /**
     * Returns the conveyor in front (the exit-point, usually a normal conveyor)
     *
     * @param blockPosition The position of Self
     * @return Conveyable The Conveyable in front
     */
    private Conveyable getConveyorInFront(BlockPos blockPosition) {
        Direction direction = getCachedState().get(Properties.FACING);
        BlockPos outputPosition = blockPosition.offset(direction.getOpposite());
        return (Conveyable) getWorld().getBlockEntity(outputPosition);
    }

    @Override
    public void setDown(boolean down) {
        this.down = down;
        markDirty();
    }

    @Override
    public int[] getRenderAttachmentData() {
        return new int[]{
                this.getPosition(),
                this.getPrevPosition(),
                this.getHorizontalPosition(),
                this.prevHorizontalPosition
        };
    }

    /**
     * Get Horizontal position
     * @return int
     */
    public int getHorizontalPosition() {
        return this.horizontalPosition;
    }

    /**
     *
     * @param horizontalPosition
     */
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
        this.down = compoundTag.getBoolean("down_vertical");
        this.horizontalPosition = compoundTag.getInt("horizontalPosition");
        this.prevHorizontalPosition = this.horizontalPosition = compoundTag.getInt("horizontalPosition");
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        fromTag(getCachedState(), compoundTag);
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        compoundTag.putBoolean("down_vertical", this.down);
        compoundTag.putInt("horizontalPosition", this.horizontalPosition);
        return super.toTag(compoundTag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return toTag(compoundTag);
    }
}
