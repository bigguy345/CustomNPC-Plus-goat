package noppes.npcs;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import noppes.npcs.api.IWorld;
import noppes.npcs.blocks.tiles.TileNpcContainer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.*;
import noppes.npcs.controllers.data.AnimationData;
import noppes.npcs.controllers.data.Frame;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.MillisTimer;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;

public class CommonProxy implements IGuiHandler {
    public final static HashSet<AnimationData> clientPlayingAnimations = new HashSet<>();
    public final static HashSet<AnimationData> serverPlayingAnimations = new HashSet<>();
    protected MillisTimer animationTimer = new MillisTimer(1000);
    protected static long totalServerTicks;
    protected static long totalClientTicks;

	public void load() {
        this.createAnimationThread();

		CustomNpcs.Channel.register(new PacketHandlerServer());
		CustomNpcs.ChannelPlayer.register(new PacketHandlerPlayer());
	}

    protected void createAnimationThread() {
        Thread thread = (new Thread("Animation Thread") { public void run() {
            while (true) {
                try {
                    if (!CustomNpcs.proxy.hasClient() || !Minecraft.getMinecraft().isGamePaused()) {
                        animationTimer.updateTimer();
                    }

                    for (int i = 0; i < animationTimer.elapsedTicks; ++i) {
                        updateData(totalClientTicks, clientPlayingAnimations);
                        totalClientTicks++;

                        updateData(totalServerTicks, serverPlayingAnimations);
                        totalServerTicks++;
                    }

                    totalClientTicks %= Long.MAX_VALUE;
                    totalServerTicks %= Long.MAX_VALUE;

                    synchronized (clientPlayingAnimations) {
                        clientPlayingAnimations.removeIf(CommonProxy.this::removeAnimation);
                    }
                    synchronized (serverPlayingAnimations) {
                        serverPlayingAnimations.removeIf(CommonProxy.this::removeAnimation);
                    }
                } catch (Exception e) {
                    if (!(e instanceof ConcurrentModificationException)) {
                        e.printStackTrace();
                    }
                }
            }
        }});
        thread.setDaemon(true);
        thread.start();
    }

    private void updateData(long ticks, Collection<AnimationData> animationData) {
        for (AnimationData data : animationData) {
            Frame frame = data.animation != null && data.animation.currentFrame() != null
                ? (Frame) data.animation.currentFrame() : null;
            if (frame != null) {
                int tickDuration = frame.tickDuration();
                if (ticks % tickDuration == 0) {
                    data.increaseTime();
                }
            }
        }
    }

    private boolean removeAnimation(AnimationData data) {
        Entity entity = null;
        if (data.parent instanceof DataDisplay) {
            entity = ((DataDisplay)data.parent).npc;
        } else {
            if (data.parent instanceof PlayerData) {
                entity = ((PlayerData) data.parent).player;
            } else if (data.parent instanceof EntityPlayer) {
                entity = (EntityPlayer) data.parent;
            }
        }

        if (entity == null) return true;

        if (entity.worldObj != null && entity.worldObj.isRemote) {
            return clientRemoveAnimation(entity);
        } else {
            return entity.worldObj == null || !entity.worldObj.loadedEntityList.contains(entity)
                || CustomNpcs.getServer().isServerStopped();
        }
    }

    @SideOnly(Side.CLIENT)
    private boolean clientRemoveAnimation(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld == null
            || mc.currentScreen == null && entity != mc.thePlayer  && !mc.theWorld.loadedEntityList.contains(entity);
    }

	public PlayerData getPlayerData(EntityPlayer player) {
		return null;
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if(ID > EnumGuiType.values().length)
			return null;
		EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
		EnumGuiType gui = EnumGuiType.values()[ID];
		return getContainer(gui, player, x, y, z, npc);
	}
	public Container getContainer(EnumGuiType gui,EntityPlayer player, int x, int y, int z,EntityNPCInterface npc){
		if(gui == EnumGuiType.CustomGui)
			return new ContainerCustomGui(new InventoryBasic("", false, x));

		if(gui == EnumGuiType.MainMenuInv)
			return new ContainerNPCInv(npc, player);

		if(gui == EnumGuiType.PlayerBankSmall)
			return new ContainerNPCBankSmall(player, x , y);

		if(gui == EnumGuiType.PlayerBankUnlock)
			return new ContainerNPCBankUnlock(player, x , y);

		if(gui == EnumGuiType.PlayerBankUprade)
			return new ContainerNPCBankUpgrade(player, x , y);

		if(gui == EnumGuiType.PlayerBankLarge)
			return new ContainerNPCBankLarge(player, x , y);

		if(gui == EnumGuiType.PlayerFollowerHire)
			return new ContainerNPCFollowerHire(npc, player);

		if(gui == EnumGuiType.PlayerFollower)
			return new ContainerNPCFollower(npc, player);

		if(gui == EnumGuiType.PlayerTrader)
			return  new ContainerNPCTrader(npc, player);

		if(gui == EnumGuiType.PlayerAnvil)
			return new ContainerCarpentryBench(player.inventory, player.worldObj, x, y, z);

		if(gui == EnumGuiType.SetupItemGiver)
			return new ContainerNpcItemGiver(npc, player);

		if(gui == EnumGuiType.SetupTrader)
			return new ContainerNPCTraderSetup(npc, player);

		if(gui == EnumGuiType.SetupFollower)
			return new ContainerNPCFollowerSetup(npc, player);

		if(gui == EnumGuiType.QuestReward)
			return new ContainerNpcQuestReward(player);

		if(gui == EnumGuiType.QuestItem)
			return new ContainerNpcQuestTypeItem(player);

		if(gui == EnumGuiType.ManageRecipes)
			return new ContainerManageRecipes(player,x);

		if(gui == EnumGuiType.ManageBanks)
			return new ContainerManageBanks(player);

		if(gui == EnumGuiType.MerchantAdd)
			return new ContainerMerchantAdd(player, ServerEventsHandler.Merchant, player.worldObj);

		if(gui == EnumGuiType.Crate)
			return new ContainerCrate(player.inventory, (TileNpcContainer)player.worldObj.getTileEntity(x, y, z));

		if(gui == EnumGuiType.PlayerMailman)
			return new ContainerMail(player, x == 1, y == 1);

		if(gui == EnumGuiType.CompanionInv)
			return new ContainerNPCCompanion(npc, player);

		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		// TODO Auto-generated method stub
		return null;
	}

	public void openGui(EntityNPCInterface npc, EnumGuiType gui) {
		// TODO Auto-generated method stub
	}

	public void openGui(EntityNPCInterface npc, EnumGuiType gui, int x, int y, int z) {
		// TODO Auto-generated method stub
	}


	public void openGui(int i, int j, int k, EnumGuiType gui, EntityPlayer player) {

	}

	public void openGui(EntityPlayer player, Object guiscreen) {
		// TODO Auto-generated method stub

	}

	public void spawnParticle(EntityLivingBase player, String string, Object... ob) {
		// TODO Auto-generated method stub

	}

	public FakePlayer getCommandPlayer(IWorld world) { return null; }

	public boolean hasClient() {
		return false;
	}

	public EntityPlayer getPlayer() {
		return null;
	}

	public void registerItem(Item item) {
		// TODO Auto-generated method stub

	}

	public ModelBiped getSkirtModel() {
		return null;
	}

	public void spawnParticle(String particle, double x, double y, double z,
							  double motionX, double motionY, double motionZ, float scale) {

	}

	public String getAchievementDesc(Achievement achievement) {
		return "";
	}

	public boolean isGUIOpen(){
		return false;
	}
}
