import JSZip from 'jszip';
import config, { absBackendBaseURL, absFrontendBaseURL } from '../common/config';
import { VaultConfigHeaderHub, VaultConfigPayload, VaultKeys } from '../common/crypto';

export class VaultConfig {
  readonly vaultConfigToken: string;
  readonly rootDirHash: string;
  readonly vaultUvf: string;

  private constructor(vaultConfigToken: string, vaultUvf: string, rootDirHash: string) {
    this.vaultConfigToken = vaultConfigToken;
    this.vaultUvf = vaultUvf;
    this.rootDirHash = rootDirHash;
  }

  public static async create(vaultId: string, vaultKeys: VaultKeys, vaultUvf: string): Promise<VaultConfig> {
    const cfg = config.get();

    const kid = `hub+${absBackendBaseURL}vaults/${vaultId}`;

    const hubConfig: VaultConfigHeaderHub = {
      clientId: cfg.keycloakClientIdCryptomator,
      authEndpoint: cfg.keycloakAuthEndpoint,
      tokenEndpoint: cfg.keycloakTokenEndpoint,
      authSuccessUrl: `${absFrontendBaseURL}unlock-success?vault=${vaultId}`,
      authErrorUrl: `${absFrontendBaseURL}unlock-error?vault=${vaultId}`,
      apiBaseUrl: absBackendBaseURL,
      devicesResourceUrl: `${absBackendBaseURL}devices/`,
    };

    const jwtPayload: VaultConfigPayload = {
      jti: vaultId,
      format: 8,
      cipherCombo: 'SIV_GCM',
      shorteningThreshold: 220
    };

    const vaultConfigToken = await vaultKeys.createVaultConfig(kid, hubConfig, jwtPayload);
    const rootDirHash = await vaultKeys.hashDirectoryId('');
    return new VaultConfig(vaultConfigToken, vaultUvf, rootDirHash);
  }

  public async exportTemplate(): Promise<Blob> {
    const zip = new JSZip();
    zip.file('vault.cryptomator', this.vaultConfigToken);
    zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
    return zip.generateAsync({ type: 'blob' });
  }

    public async exportMetadataTemplate(): Promise<Blob> {
      const zip = new JSZip();
      zip.file('vault.uvf', this.vaultUvf);
      zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
      return zip.generateAsync({ type: 'blob' });
    }
}
