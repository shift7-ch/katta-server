package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Authority;
import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// / start cipherduck extension
// TODO review: backport @Schema upstream?
@Schema(
		title = "Authority",
		oneOf = { UserDto.class, GroupDto.class, MemberDto.class },
		discriminatorMapping = {
				@DiscriminatorMapping( value = "USER", schema = UserDto.class ),
				@DiscriminatorMapping( value = "GROUP", schema = GroupDto.class ),
				@DiscriminatorMapping( value = "MEMBER", schema = MemberDto.class )
		},
		discriminatorProperty = "type"
)
// \ end cipherduck extension
abstract sealed class AuthorityDto permits UserDto, GroupDto, MemberDto {

	public enum Type {
		USER, GROUP
	}

	@JsonProperty("id")
	public final String id;

	@JsonProperty("type")
	public final Type type;

	@JsonProperty("name")
	public final String name;

	@JsonProperty("pictureUrl")
	public final String pictureUrl;

	protected AuthorityDto(String id, Type type, String name, String pictureUrl) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.pictureUrl = pictureUrl;
	}

	static AuthorityDto fromEntity(Authority a) {
		return switch (a) {
			case User u -> UserDto.justPublicInfo(u);
			case Group g -> GroupDto.fromEntity(g);
			default -> throw new IllegalStateException("authority is not of type user or group");
		};
	}

}
