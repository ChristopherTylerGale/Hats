package hats.common.core;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hats.common.Hats;
import hats.common.packet.PacketPing;
import hats.common.packet.PacketSession;
import ichun.client.keybind.KeyEvent;
import ichun.common.core.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayList;
import java.util.List;

public class EventHandler
{
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyEvent(KeyEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(event.keyBind.isPressed() && event.keyBind == Hats.config.getKeyBind("guiKeyBind"))
        {
            if(mc.currentScreen == null && !Hats.proxy.tickHandlerClient.hasScreen)
            {
                if(Hats.config.getSessionInt("playerHatsMode") == 3)
                {
                    PacketHandler.sendToServer(Hats.channels, new PacketPing(0, false));
                }
                else if(Hats.config.getSessionInt("playerHatsMode") == 2)
                {
                    mc.thePlayer.addChatMessage(new ChatComponentTranslation("hats.lockedMode"));
                }
                else if(Hats.config.getSessionInt("playerHatsMode") == 5 && !Hats.config.getSessionString("currentKing").equalsIgnoreCase(mc.thePlayer.getCommandSenderName()))
                {
                    mc.thePlayer.addChatMessage(new ChatComponentTranslation("hats.kingOfTheHat.notKing", Hats.config.getSessionString("currentKing")));
                }
                else
                {
                    Hats.proxy.openHatsGui();
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && event.world.provider.dimensionId == 0)
        {
            Hats.proxy.loadData((WorldServer)event.world);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && event.world.provider.dimensionId == 0)
        {
            Hats.proxy.saveData((WorldServer)event.world);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event)
    {
        if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
        {
            if(Hats.config.getSessionInt("playerHatsMode") >= 4)
            {
                if(Hats.config.getSessionInt("playerHatsMode") == 4)
                {
                    if(!(event.entityLiving instanceof EntityPlayer) && event.source.getEntity() instanceof EntityPlayer && !((EntityPlayer)event.source.getEntity()).capabilities.isCreativeMode)
                    {
                        Hats.proxy.tickHandlerServer.playerKilledEntity(event.entityLiving, (EntityPlayer)event.source.getEntity());
                    }
                }

                if(event.entityLiving instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer)event.entityLiving;
                    EntityPlayer executer = null;
                    if(event.source.getEntity() instanceof EntityPlayer)
                    {
                        executer = (EntityPlayer)event.source.getEntity();
                    }
                    if(Hats.config.getSessionInt("playerHatsMode") == 5)
                    {
                        //King died
                        if(Hats.config.getSessionString("currentKing").equalsIgnoreCase(player.getCommandSenderName()))
                        {
                            if(executer != null)
                            {
                                Hats.proxy.tickHandlerServer.updateNewKing(executer.getCommandSenderName(), null, true);
                                Hats.proxy.tickHandlerServer.updateNewKing(executer.getCommandSenderName(), executer, true);
                                FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("hats.kingOfTheHat.update.playerSlayed", new Object[] { player.getCommandSenderName(), executer.getCommandSenderName() }));
                            }
                            else
                            {
                                List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().playerEntityList;
                                List<EntityPlayerMP> list = new ArrayList(players);
                                list.remove(player);
                                if(!list.isEmpty())
                                {
                                    EntityPlayer newKing = list.get(player.worldObj.rand.nextInt(list.size()));
                                    Hats.proxy.tickHandlerServer.updateNewKing(newKing.getCommandSenderName(), null, true);
                                    Hats.proxy.tickHandlerServer.updateNewKing(newKing.getCommandSenderName(), newKing, true);
                                    FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("hats.kingOfTheHat.update.playerDied", new Object[] { player.getCommandSenderName(), newKing.getCommandSenderName() }));
                                }
                            }
                        }
                        else if(executer != null && Hats.config.getSessionString("currentKing").equalsIgnoreCase(executer.getCommandSenderName()))
                        {
                            ArrayList<String> playerHatsList = Hats.proxy.tickHandlerServer.playerHats.get(executer.getCommandSenderName());
                            if(playerHatsList == null)
                            {
                                playerHatsList = new ArrayList<String>();
                                Hats.proxy.tickHandlerServer.playerHats.put(executer.getCommandSenderName(), playerHatsList);
                            }

                            ArrayList<String> newHats = HatHandler.getAllHatsAsList();

                            newHats.removeAll(playerHatsList);

                            EntityPlayerMP newKingEnt = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().getPlayerForUsername(executer.getCommandSenderName());

                            if(newKingEnt != null && !newHats.isEmpty())
                            {
                                HatHandler.unlockHat(newKingEnt, newHats.get(newKingEnt.worldObj.rand.nextInt(newHats.size())));
                            }
                        }
                    }

                    if(Hats.config.getInt("resetPlayerHatsOnDeath") == 1)
                    {
                        Hats.proxy.tickHandlerServer.playerDeath((EntityPlayer)event.entityLiving);
                    }
                }
            }
            Hats.proxy.tickHandlerServer.mobHatsToRemove.add(event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event)
    {
        Hats.proxy.tickHandlerClient.isActive = true;

        Hats.config.resetSession();

        Hats.config.updateSession("serverHasMod", 0);
        Hats.config.updateSession("playerHatsMode", 1);
        Hats.config.updateSession("hasVisited", 1);
        Hats.config.updateSession("lockedHat", "");
        Hats.config.updateSession("currentKing", "");

        Hats.config.updateSession("showJoinMessage", 0);
        HatHandler.repopulateHatsList();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        Hats.proxy.tickHandlerClient.hats.clear();
        Hats.proxy.tickHandlerClient.mobHats.clear();
        Hats.proxy.tickHandlerClient.playerWornHats.clear();
        Hats.proxy.tickHandlerClient.requestedHats.clear();
        if(Hats.proxy.tickHandlerClient.guiHatUnlocked != null)
        {
            Hats.proxy.tickHandlerClient.guiHatUnlocked.hatList.clear();
        }
        if(Hats.proxy.tickHandlerClient.guiNewTradeReq != null)
        {
            Hats.proxy.tickHandlerClient.guiNewTradeReq.hatList.clear();
        }
        Hats.proxy.tickHandlerClient.worldInstance = null;
    }

    public static void sendPlayerSessionInfo(EntityPlayer player)
	{
        ArrayList<String> playerHatsList = Hats.proxy.tickHandlerServer.playerHats.get(player.getCommandSenderName());
        if(playerHatsList == null)
        {
            playerHatsList = new ArrayList<String>();
            Hats.proxy.tickHandlerServer.playerHats.put(player.getCommandSenderName(), playerHatsList);
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < playerHatsList.size(); i++)
        {
            sb.append(playerHatsList.get(i));
            if(i < playerHatsList.size() - 1)
            {
                sb.append(":");
            }
        }

        PacketHandler.sendToPlayer(Hats.channels, new PacketSession(Hats.config.getSessionInt("playerHatsMode"), Hats.proxy.saveData != null && Hats.proxy.saveData.getBoolean(player.getCommandSenderName() + "_hasVisited") && Hats.proxy.saveData.getInteger(player.getCommandSenderName() + "_hatMode") == Hats.config.getSessionInt("playerHatsMode") || Hats.config.getInt("firstJoinMessage") != 1, Hats.config.getSessionString("lockedHat"), Hats.config.getSessionString("currentKing"), sb.toString()), player);
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
	{
		if(Hats.config.getSessionInt("playerHatsMode") == 5 && Hats.config.getSessionString("currentKing").equalsIgnoreCase(""))
		{
			//There is No king around now, so technically no players online
			Hats.proxy.tickHandlerServer.updateNewKing(event.player.getCommandSenderName(), null, false);
			FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("hats.kingOfTheHat.update.playerJoin", event.player.getCommandSenderName()));
		}

		if(Hats.proxy.saveData != null)
		{
			String playerHats = Hats.proxy.saveData.getString(event.player.getCommandSenderName() + "_unlocked");
			
			if(Hats.config.getSessionInt("playerHatsMode") == 5)
			{
				if(!Hats.config.getSessionString("currentKing").equalsIgnoreCase(event.player.getCommandSenderName()))
				{
					playerHats = "";
				}
			}
			
			ArrayList<String> playerHatsList = Hats.proxy.tickHandlerServer.playerHats.get(event.player.getCommandSenderName());
			if(playerHatsList == null)
			{
				playerHatsList = new ArrayList<String>();
				Hats.proxy.tickHandlerServer.playerHats.put(event.player.getCommandSenderName(), playerHatsList);
			}
			
			playerHatsList.clear();
			String[] hats = playerHats.split(":");
			for(String hat : hats)
			{
				if(!hat.trim().equalsIgnoreCase(""))
				{
					boolean has = false;
					for(String s : playerHatsList)
					{
						if(s.equalsIgnoreCase(hat))
						{
							has = true;
							break;
						}
					}
					if(!has)
					{
						playerHatsList.add(hat);
					}
				}
			}
			
			String hatName = Hats.proxy.saveData.getString(event.player.getCommandSenderName() + "_wornHat");
			int r = Hats.proxy.saveData.getInteger(event.player.getCommandSenderName() + "_colourR");
			int g = Hats.proxy.saveData.getInteger(event.player.getCommandSenderName() + "_colourG");
			int b = Hats.proxy.saveData.getInteger(event.player.getCommandSenderName() + "_colourB");
			
			if(!HatHandler.hasHat(hatName))
			{
				HatHandler.requestHat(hatName, event.player);
			}
			
			Hats.proxy.playerWornHats.put(event.player.getCommandSenderName(), new HatInfo(hatName, r, g, b));
			
			if(Hats.config.getSessionInt("playerHatsMode") == 6)
			{
				TimeActiveInfo info = Hats.proxy.tickHandlerServer.playerActivity.get(event.player.getCommandSenderName());
				
				if(info == null)
				{
					info = new TimeActiveInfo();
					info.timeLeft = Hats.proxy.saveData.getInteger(event.player.getCommandSenderName() + "_activityTimeleft");
					info.levels = Hats.proxy.saveData.getInteger(event.player.getCommandSenderName() + "_activityLevels");
					
					if(info.levels == 0 && info.timeLeft == 0)
					{
						info.levels = 0;
						info.timeLeft = Hats.config.getInt("startTime");
					}
					
					Hats.proxy.tickHandlerServer.playerActivity.put(event.player.getCommandSenderName(), info);
				}
				
				info.active = true;
			}
		}
		else
		{
			Hats.proxy.playerWornHats.put(event.player.getCommandSenderName(), new HatInfo());
		}
		
		sendPlayerSessionInfo(event.player);
		
		Hats.proxy.saveData.setBoolean(event.player.getCommandSenderName() + "_hasVisited", true);
		Hats.proxy.saveData.setInteger(event.player.getCommandSenderName() + "_hatMode", Hats.config.getSessionInt("playerHatsMode"));

		if(Hats.config.getSessionInt("playerHatsMode") != 2)
		{
			Hats.proxy.sendPlayerListOfWornHats(event.player, true);
			Hats.proxy.sendPlayerListOfWornHats(event.player, false);
		}
	}

	@SubscribeEvent
	public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if(Hats.config.getSessionInt("playerHatsMode") == 5 && Hats.config.getSessionString("currentKing").equalsIgnoreCase(event.player.getCommandSenderName()))
		{
			//King logged out
			List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().playerEntityList;
			List<EntityPlayerMP> list = new ArrayList(players);
			list.remove(event.player);
			if(!list.isEmpty())
			{
				EntityPlayer newKing = list.get(event.player.worldObj.rand.nextInt(list.size()));
				Hats.proxy.tickHandlerServer.updateNewKing(newKing.getCommandSenderName(), null, true);
				Hats.proxy.tickHandlerServer.updateNewKing(newKing.getCommandSenderName(), newKing, true);
				FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("hats.kingOfTheHat.update.playerLeft", new Object[] { event.player.getCommandSenderName(), newKing.getCommandSenderName() }));
			}
		}	
		
		TimeActiveInfo info = Hats.proxy.tickHandlerServer.playerActivity.get(event.player.getCommandSenderName());

		if(info != null)
		{
			info.active = false;
		}
		
		Hats.proxy.playerWornHats.remove(event.player.getCommandSenderName());
	}
}
