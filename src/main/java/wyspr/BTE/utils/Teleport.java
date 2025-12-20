package wyspr.BTE.utils;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.net.packet.PacketAddEntity;
import net.minecraft.core.net.packet.PacketRespawn;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;
import net.minecraft.server.world.WorldServer;

import wyspr.BTE.Essentials;

public class Teleport {
	public static boolean teleport(Player player, double x, double y, double z, int dimID) {
		return teleport(player, new WorldPosition(x, y, z, dimID));
	}

	public static boolean teleport(Player player, WorldPosition destination) {
		if (player.isPassenger()) {
			player.sendMessage(TextFormatting.YELLOW + "You can't teleport while you're a passenger.");
			return false;
		}

		PlayerList playerList = MinecraftServer.getInstance().playerList;
		if (player.dimension != destination.dimID) {
			playerList.sendPlayerToOtherDimension(
				(PlayerServer) player,
				destination.dimID,
				DyeColor.MAGENTA,
				false
			);
			playerList.sendPacketToPlayer(
				player.username, new PacketRespawn(
					(byte) destination.dimID,
					(byte) 0
				)
			);
		}

		MinecraftServer server     = MinecraftServer.getInstance();
		WorldServer     world      = server.getDimensionWorld(destination.dimID);
		int             chunkCordX = (int) destination.x >> 4;
		int             chunkCordZ = (int) destination.z >> 4;

		world
			.getChunkProvider()
			.prepareChunk(chunkCordX, chunkCordZ);


		((PlayerServer) player).teleport(
			destination.x,
			destination.y,
			destination.z,
			player.yRot,
			player.xRot
		);
		player.moveTo(destination.x, destination.y, destination.z, player.yRot, player.xRot);
		player.world.playSoundAtEntity(null, player, Essentials.TeleportSound, 2, 2);
		player.world.spawnParticle("smoke", destination.x + 0.5, destination.y, destination.z + 0.5, 0, 0, 0, 0);
		// Show the teleported player  instantly
		// instead of waiting on the server to send the packet
		playerList.sendPacketToPlayersAroundPoint(
			destination.x,
			destination.y,
			destination.z,
			64,
			destination.dimID,
			new PacketAddEntity(player)
		);

		return true;
	}

	public static boolean teleport(Player movingPlayer, Player stationaryPlayer) {
		if (movingPlayer.isPassenger() || stationaryPlayer.isPassenger()) {
			movingPlayer.sendMessage(TextFormatting.YELLOW + "You cannot teleport as, or to, a passenger!");
			stationaryPlayer.sendMessage(TextFormatting.YELLOW + "You cannot teleport as, or to, a passenger!");
			return false;
		}
		double     x          = stationaryPlayer.x;
		double     y          = stationaryPlayer.y;
		double     z          = stationaryPlayer.z;
		float      xr         = stationaryPlayer.xRot;
		float      yr         = stationaryPlayer.yRot;
		PlayerList playerList = MinecraftServer.getInstance().playerList;
		if (movingPlayer.dimension != stationaryPlayer.dimension) {
			playerList.sendPlayerToOtherDimension(
				(PlayerServer) movingPlayer,
				stationaryPlayer.dimension,
				null,
				false
			);
			playerList.sendPacketToPlayer(
				movingPlayer.username, new PacketRespawn(
					(byte) stationaryPlayer.dimension,
					(byte) 0
				)
			);
		}
		((PlayerServer) movingPlayer).teleport(x, y, z, yr, xr);
		movingPlayer.moveTo(x, y, z, yr, xr);
		movingPlayer.world.playSoundAtEntity(null, movingPlayer, Essentials.TeleportSound, 2, 2);
		movingPlayer.world.spawnParticle("smoke", x + 0.5, y, z + 0.5, 0, 0, 0, 0);
		// Show the teleported player to the accepting player instantly
		// instead of waiting on the server to send the packet
		playerList.sendPacketToPlayersAroundPoint(
			x,
			y,
			z,
			64,
			stationaryPlayer.dimension,
			new PacketAddEntity(movingPlayer)
		);
		return true;
	}
}
