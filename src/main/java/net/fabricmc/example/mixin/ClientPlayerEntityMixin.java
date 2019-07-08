package net.fabricmc.example.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstInitializer;
import net.wurstclient.events.ChatOutputListener.ChatOutputEvent;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
{
	public ClientPlayerEntityMixin(WurstClient wurst, ClientWorld clientWorld_1,
		GameProfile gameProfile_1)
	{
		super(clientWorld_1, gameProfile_1);
	}
	
	@Inject(at = @At("HEAD"),
		method = "sendChatMessage(Ljava/lang/String;)V",
		cancellable = true)
	private void onSendChatMessage(String message, CallbackInfo ci)
	{
		ChatOutputEvent event = new ChatOutputEvent(message, false);
		WurstInitializer.getWurst().getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
}