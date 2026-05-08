package org.cryptomator.hub.entities.katta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "storage_profile")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "protocol", discriminatorType = DiscriminatorType.STRING)
public class StorageProfile {

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

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<StorageProfile, UUID> {
	}
}
