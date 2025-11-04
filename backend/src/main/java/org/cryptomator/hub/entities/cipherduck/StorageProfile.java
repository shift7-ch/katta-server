package org.cryptomator.hub.entities.cipherduck;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.cryptomator.hub.api.cipherduck.StorageProfileDto;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class StorageProfile extends PanacheEntityBase { // TODO make sealed?
	@Id
	@Column(name = "id", nullable = false)
	public UUID id;

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "archived", nullable = false)
	public boolean archived;

	@Column(name = "protocol", nullable = false)
	public StorageProfileDto.Protocol protocol;

	public StorageProfile setArchived(boolean archived) {
		this.archived = archived;
		return this;
	}
}
