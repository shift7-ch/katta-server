import JSZip from 'jszip';
// / cipherduck modification
// import config, { absBackendBaseURL, absFrontendBaseURL } from '../common/config';
// import { VaultConfigHeaderHub, VaultConfigPayload, VaultKeys } from '../common/crypto';
// \ end cipherduck modification
export class VaultConfig {
  // / cipherduck modification
  readonly vaultUvf: string;
  readonly rootDirHash: string;
  // \ cipherduck modification

  private constructor(vaultUvf: string, rootDirHash: string) {
    this.vaultUvf = vaultUvf;
    this.rootDirHash = rootDirHash;
  }

  // / start cipherduck modification
  public static async create(vaultId: string, rootDirHash: string, vaultUvf: string): Promise<VaultConfig> {
    return new VaultConfig(vaultUvf, rootDirHash);
  }

  public async exportTemplate(): Promise<Blob> {
    const zip = new JSZip();
    zip.file('vault.uvf', this.vaultUvf);
    zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
    return zip.generateAsync({ type: 'blob' });
  }
  // \ end cipherduck modification
}
