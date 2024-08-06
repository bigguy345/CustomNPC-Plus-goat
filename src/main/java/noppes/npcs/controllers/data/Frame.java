package noppes.npcs.controllers.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.api.handler.data.IFrame;
import noppes.npcs.api.handler.data.IFramePart;
import noppes.npcs.constants.EnumAnimationPart;

import java.util.HashMap;
import java.util.Map;


public class Frame implements IFrame {
	public Animation parent;
	public HashMap<EnumAnimationPart,FramePart> frameParts = new HashMap<>();
	public int duration = 0;
    public int tickDuration = 50;

	boolean customized = false;
	public float speed = 1.0F;
	public byte smooth = 0;

	private int colorMarker = 0xFFFFFF;
    private String comment = "";

	public Frame(){}

	public Frame(int duration) {
		this.duration = duration;
	}

	public Frame(int duration, float speed, byte smooth) {
		this.duration = duration;
		this.speed = speed;
		this.smooth = smooth;
		this.customized = true;
	}

	public HashMap<EnumAnimationPart,FramePart> getFrameMap() {
		return frameParts;
	}

	public IFramePart[] getParts() {
		return frameParts.values().toArray(new IFramePart[0]);
	}

	public IFrame addPart(IFramePart partConfig) {
		this.frameParts.put(((FramePart)partConfig).getPart(),(FramePart) partConfig);
		return this;
	}

	public IFrame removePart(String partName) {
		try {
			this.frameParts.remove(EnumAnimationPart.valueOf(partName));
		} catch (IllegalArgumentException ignored) {}
		return this;
	}

	public IFrame removePart(int partId) {
		for (EnumAnimationPart part : EnumAnimationPart.values()) {
			this.frameParts.remove(part);
		}
		return this;
	}

	public IFrame clearParts() {
		frameParts.clear();
		return this;
	}

	public int getDuration() {
		return duration;
	}

	public IFrame setDuration(int duration) {
		this.duration = duration;
		return this;
	}

    public int tickDuration() {
        return !this.customized && this.parent != null ? this.parent.tickDuration : this.tickDuration;
    }

    public IFrame setTickDuration(int tickDuration) {
        this.tickDuration = tickDuration;
        return this;
    }

	public boolean isCustomized() {
		return customized;
	}

	public IFrame setCustomized(boolean customized) {
		this.customized = customized;
		return this;
	}

	public float getSpeed() {
		return speed;
	}

	public IFrame setSpeed(float speed) {
		this.speed = speed;
		return this;
	}

	public byte smoothType() {
		return smooth;
	}

	public IFrame setSmooth(byte smooth) {
		this.smooth = smooth;
		return this;
	}

	public int getColorMarker() {
		return this.colorMarker;
	}

	public void setColorMarker(int color) {
		this.colorMarker = color;
	}

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

	public void readFromNBT(NBTTagCompound compound){
		duration = compound.getInteger("Duration");
		if (compound.hasKey("ColorMarker")) {
			this.setColorMarker(compound.getInteger("ColorMarker"));
		}
        if (compound.hasKey("Comment")) {
            this.comment = compound.getString("Comment");
        }

		// Customized = TRUE if Speed or Smooth Exist
		if(compound.hasKey("Speed")){
			customized = true;
			speed = compound.getFloat("Speed");
		}
		if(compound.hasKey("Smooth")){
			customized = true;
			smooth = compound.getByte("Smooth");
		}

        if (compound.hasKey("TickDuration")) {
            customized = true;
            this.tickDuration = compound.getInteger("TickDuration");
        } else if(compound.hasKey("RenderTicks")){
			customized = true;
            this.tickDuration = compound.getBoolean("RenderTicks") ? 20 : 50;
		}

		if (!customized && parent != null) {
			this.speed = parent.speed;
			this.smooth = parent.smooth;
			this.tickDuration = parent.tickDuration;
		}

		HashMap<EnumAnimationPart,FramePart> frameParts = new HashMap<>();
		NBTTagList list = compound.getTagList("FrameParts", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound item = list.getCompoundTagAt(i);
			FramePart framePart = new FramePart();
			framePart.readFromNBT(item);
			if (!framePart.customized) {
				framePart.smooth = this.smooth;
				framePart.speed = this.speed;
			}
			frameParts.put(framePart.part,framePart);
		}
		this.frameParts = frameParts;
	}

	public NBTTagCompound writeToNBT(){
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("Duration", duration);
		compound.setInteger("ColorMarker", this.colorMarker);
        compound.setString("Comment", this.comment);

		if(customized){
			compound.setFloat("Speed", speed);
			compound.setByte("Smooth", smooth);
			compound.setInteger("TickDuration", tickDuration);
		}

		NBTTagList list = new NBTTagList();
		for(FramePart framePart : frameParts.values()){
			NBTTagCompound item = framePart.writeToNBT();
			list.appendTag(item);
		}
		compound.setTag("FrameParts", list);
		return compound;
	}

	public Frame copy() {
		Frame frame = new Frame(this.duration);
		HashMap<EnumAnimationPart,FramePart> frameParts = this.frameParts;
		for (Map.Entry<EnumAnimationPart,FramePart> entry : frameParts.entrySet()) {
			frame.frameParts.put(entry.getKey(),entry.getValue().copy());
		}
		frame.parent = this.parent;
		frame.duration = this.duration;
		frame.customized = this.customized;
		frame.speed = this.speed;
		frame.smooth = this.smooth;
		frame.tickDuration = this.tickDuration;
		frame.colorMarker = this.colorMarker;
		return frame;
	}
}
