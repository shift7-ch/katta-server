package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "create_vault_event")
@DiscriminatorValue(CreateVaultEvent.TYPE)
public class CreateVaultEvent extends AuditEvent {

	public static final String TYPE = "CREATE_VAULT";

	@Column(name = "user_id")
	public String userId;

	@Column(name = "vault_id")
	public UUID vaultId;

	@Column(name = "name")
	public String name;

	@Column(name = "description")
	public String description;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CreateVaultEvent that = (CreateVaultEvent) o;
		return super.equals(that) //
				&& Objects.equals(userId, that.userId) //
				&& Objects.equals(vaultId, that.vaultId) //
				&& Objects.equals(name, that.name) //
				&& Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, vaultId);
	}

	public static void log(String userId, UUID vaultId, String name, String description) {
		var event = new CreateVaultEvent();
		event.timestamp = Instant.now();
		event.userId = userId;
		event.vaultId = vaultId;
		event.name = name;
		event.description = description;
		event.persist();
	}

}
