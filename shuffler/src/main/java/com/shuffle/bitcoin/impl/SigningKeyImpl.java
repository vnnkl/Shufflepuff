package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;

import javax.annotation.Nonnull;

/**
 * Created by conta on 10.03.16.
 */
public class SigningKeyImpl implements SigningKey {

    final ECKey signingKey;
    final NetworkParameters params;
    private final VerificationKeyImpl vk;

    public SigningKeyImpl(@Nonnull org.bitcoinj.core.ECKey ecKey,
                          @Nonnull NetworkParameters params) {
        signingKey = ecKey;
        this.params = params;
        vk = new VerificationKeyImpl(signingKey.getPubKey(), params);
    }

    public SigningKeyImpl(@Nonnull String s) throws AddressFormatException {
        Bytestring stripped = new Bytestring(Base58.decodeChecked(s));

        boolean compressed;
        switch (stripped.bytes.length) {
            case (34) : {
                if (stripped.bytes[33] != 1) {
                    throw new AddressFormatException("Wrong compressed byte");
                }
                stripped = stripped.drop(-1);
                compressed = true;
                break;
            }
            case (33) : {
                compressed = false;
                break;
            }
            default : {
                throw new AddressFormatException("Invalid length.");
            }
        }

        switch (stripped.bytes[0]) {
            case (-1) : {
                params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
                break;
            }
            case (-17) : {
                params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
                break;
            }
            default: {
                throw new AddressFormatException("Invalid net byte.");
            }
        }

        stripped = stripped.drop(1);
        signingKey = ECKey.fromPrivate(stripped.bytes, compressed);
        vk = new VerificationKeyImpl(signingKey.getPubKey(), params);
    }

    public SigningKeyImpl(String s, NetworkParameters params) throws AddressFormatException {
        this(s);
        if (!this.params.equals(params)) {
            throw new AddressFormatException("Wrong network.");
        }
    }


   // returns Private Key in WIF Compressed 52 characters base58
   public String toString() {
       return this.signingKey.getPrivateKeyAsWiF(params);
   }

   @Override
   public VerificationKey VerificationKey() {
      return vk;
   }

    @Override
    public Bytestring sign(Bytestring string) {
        ECKey.ECDSASignature ecdsaSignature = signingKey.sign(Sha256Hash.of(string.bytes));
        return new Bytestring(ecdsaSignature.encodeToDER());
    }


    @Override
    public int compareTo(@Nonnull Object o) {
        if (!(o instanceof SigningKeyImpl)) {
            throw new IllegalArgumentException("unable to compare with other SingingKey");
        }
        //get netParams to create correct address and check by address.
        return vk.address.compareTo(((SigningKeyImpl) o).vk.address);
    }

   @Override
   public boolean equals(Object o) {
       return this == o || !(o == null || getClass() != o.getClass()) && toString().equals(o.toString());

   }

   @Override
   public int hashCode() {
       return toString().hashCode();
   }
}
