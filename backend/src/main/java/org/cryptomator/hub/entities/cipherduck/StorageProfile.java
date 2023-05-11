package org.cryptomator.hub.entities.cipherduck;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "storage_profile")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "protocol")
public class StorageProfile extends PanacheEntityBase { // TODO make sealed?
	@Id
	@Column(name = "id", nullable = false)
	public UUID id;

	@Column(name = "name", nullable = false)
	public String name;

	@Column(name = "archived", nullable = false)
	public boolean archived;

	public StorageProfile setArchived(boolean archived) {
		this.archived = archived;
		return this;
	}
}
