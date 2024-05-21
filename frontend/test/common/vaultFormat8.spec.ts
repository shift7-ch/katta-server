import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { UnwrapKeyError, UserKeys } from '../../src/common/crypto';
import { VaultFormat8 } from '../../src/common/vaultFormat8';

chaiUse(chaiAsPromised);

// key coordinates from MDN examples:
const alicePublic: JsonWebKey = {
  kty: 'EC',
  crv: 'P-384',
  x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
  y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'
};
const alicePrivate: JsonWebKey = {
  ...alicePublic,
  d: 'wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_',
};

describe('Vault Format 8', () => {
  let testVault: VaultFormat8;
  let alice: UserKeys;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: we only need a subset of the properties available in global.window
    global.window = { crypto: global.crypto };

    // prepare test vault with hard-coded symmetric key:
    testVault = await TestVaultKeys.create();

    // prepare some test key pairs:
    const alicePrv = crypto.subtle.importKey('jwk', alicePrivate, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const alicePub = crypto.subtle.importKey('jwk', alicePublic, UserKeys.KEY_DESIGNATION, true, []);
    alice = new TestUserKeys({ privateKey: await alicePrv, publicKey: await alicePub });
  });

  it('create()', async () => {
    const orig = await VaultFormat8.create();

    expect(orig).to.be.not.null;
  });

  it('recover() succeeds for valid actual key', async () => {
    let recoveryKey = `
      pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe charity
      border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed
      investor boot depth left theory snow whereby terminal weekly reject happiness circuit partial cup ad
      `;

    const recovered = await VaultFormat8.recover(recoveryKey);

    const newMasterKey = await crypto.subtle.exportKey('jwk', recovered.masterKey);
    expect(newMasterKey.k).to.eq('uwHiVreDbmv47K7oZzlwZbHcEql2Z29brbgFxKA7i54pXVPoHoxKK5rzZS3VEhPxHegQKCwa5Mk4ep7OsYutAw');
  });

  it('recover() succeeds for valid test key', async () => {
    let recoveryKey = `
      water water water water water water water water water water water water water water water water water
      water water water water asset partly partly partly partly partly partly partly partly partly partly
      partly partly partly partly partly partly partly partly partly partly option twist
      `;

    const recovered = await VaultFormat8.recover(recoveryKey);

    const recoveredKey = await crypto.subtle.exportKey('jwk', recovered.masterKey);
    expect(recoveredKey.k).to.eq('VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3d3dw');
  });

  it('recover() fails for invalid recovery key', async () => {
    const noMultipleOfTwo = VaultFormat8.recover('pathway');
    const notInDict = VaultFormat8.recover('hallo bonjour');
    const wrongLength = VaultFormat8.recover('pathway lift');
    const invalidCrc = VaultFormat8.recover(`
      pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe charity 
      border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed 
      investor boot depth left theory snow whereby terminal weekly reject happiness circuit partial cup wrong
      `);

    return Promise.all([
      expect(noMultipleOfTwo).to.be.rejectedWith(Error, /input needs to be a multiple of two words/),
      expect(notInDict).to.be.rejectedWith(Error, /Word not in dictionary/),
      expect(wrongLength).to.be.rejectedWith(Error, /Invalid recovery key length/),
      expect(invalidCrc).to.be.rejectedWith(Error, /Invalid recovery key checksum/),
    ]);
  });

  it('encryptForUser()', async () => {
    const encrypted = await testVault.encryptForUser(alice.keyPair.publicKey);

    expect(encrypted).to.be.not.null;
  });

  it('createRecoveryKey()', async () => {
    const recoveryKey = await testVault.createRecoveryKey();

    expect(recoveryKey).to.eql('water water water water water water water water water water water water water water water water water water water water water asset partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly option twist');
  });

  describe('Prove Vault Ownership using Vault Admin Password', () => {
    const wrapped = {
      wrappedMasterkey: 'CMPyJiiOQXBZ8FVvFZs6UOh0kW83+eALeK3bwXfFF2CWsguJZIgCJch94liWCh9xTqW84LUZPyo6IDWbSALqbbdiwDcztT8M81/pgadhTETVtHO5Q1CFNLJ9UvY=',
      wrappedOwnerPrivateKey: 'O9snY73/eVElnWRLgM404KH7WwO/Ed30Y0UrQQw6x3vxOdroJcjvPdJeSqLD2x4lVP7ceTjVt3IT2N9Mx+jhUQzqrb1E2EvEYlXrTaID1jSdBXZ6ScrI1RvU0iH9cfXf2cRy2x8QZvJyVMr34gLJ3Di/XGrnc/BrOm+aF2K4F9FJXvJFen3CnAs9ewB3Vk0A1wRLX3hW/Wx7eXt/0i1gxB8T/NcLu7xIU3+uusTHh9uajFkA5+z1+JgNHURaa1bT8j5WTtNWIHYT/sw+erMn6S0Uj1vL',
      ownerPublicKey: 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW',
      salt: 'IdXyKICznXKm41gSb5OqfQ',
      iterations: 1
    };

    it('decryptWithAdminPassword() fails with wrong pw', () => {
      return expect(VaultFormat8.decryptWithAdminPassword('wrong', wrapped.wrappedMasterkey, wrapped.wrappedOwnerPrivateKey, wrapped.ownerPublicKey, wrapped.salt, wrapped.iterations)).to.eventually.be.rejectedWith(UnwrapKeyError);
    });

    it('decryptWithAdminPassword() succeeds with correct pw', () => {
      return expect(VaultFormat8.decryptWithAdminPassword('pass', wrapped.wrappedMasterkey, wrapped.wrappedOwnerPrivateKey, wrapped.ownerPublicKey, wrapped.salt, wrapped.iterations)).to.eventually.be.fulfilled;
    });
  });

  describe('Hash directory id', () => {
    it('root directory', async () => {
      const result = await testVault.hashDirectoryId('');
      expect(result).to.eql('VLWEHT553J5DR7OZLRJAYDIWFCXZABOD');
    });

    it('specific directory', async () => {
      const result = await testVault.hashDirectoryId('918acfbd-a467-3f77-93f1-f4a44f9cfe9c');
      expect(result).to.eql('7C3USOO3VU7IVQRKFMRFV3QE4VEZJECV');
    });
  });
});

// #region Mocks

class TestVaultKeys extends VaultFormat8 {
  constructor(masterKey: CryptoKey) {
    super(masterKey);
  }

  static async create() {
    const raw = new Uint8Array(64);
    raw.fill(0x55, 0, 32);
    raw.fill(0x77, 32, 64);
    const key = await crypto.subtle.importKey(
      'raw',
      raw,
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
      true,
      ['sign']
    );
    return new TestVaultKeys(key);
  }
}

class TestUserKeys extends UserKeys {
  public constructor(keyPair: CryptoKeyPair) {
    super(keyPair);
  }
}

// #endregion