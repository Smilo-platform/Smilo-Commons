package io.smilo.commons.block.data.transaction;

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.ledger.AddressUtility;
import io.smilo.commons.ledger.MerkleTreeGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Arrays;

@Category({StableTests.class})
public class TransactionSigningTest  extends AbstractSpringTest {

    @Autowired
    private TransactionParser transactionParser;
    @Autowired
    private MerkleTreeGenerator merkelGen;
    @Autowired
    private AddressManager addressManager;
    @Autowired
    private AddressUtility addressUtility;

    @Test
    public void testIsValidWrongMerkleSignature() {
        Transaction transaction = new Transaction(1527514557052L, "000x00123", "S16WKSPIOD2SWPXJFQM4UTQ7P33R4YMMMGGDZP", BigInteger.valueOf(10L), BigInteger.ZERO,
                Arrays.asList(new TransactionOutput("S2RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCNDV2", BigInteger.valueOf(10L))),
                "424045E72D0DB035D3E41EDFC447765D18167F04FD11AD15656082067F18B5A6",
                "wrongsignature", 0L);
        String privateKey = addressManager.getDefaultPrivateKey();
        transactionParser.hash(transaction);
        transactionParser.sign(transaction, privateKey, transaction.getSignatureIndex().intValue());
        Assert.assertFalse(transactionParser.isValid(transaction));
    }

    @Test
    public void testIsValidTransaction() {
        Transaction transaction = new Transaction(1527514557052L, "000x00123", "S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR", BigInteger.valueOf(10L), BigInteger.ZERO,
                Arrays.asList(new TransactionOutput("S2RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCNDV2", BigInteger.valueOf(10L))),
                "424045E72D0DB035D3E41EDFC447765D18167F04FD11AD15656082067F18B5A6",
                "", 0L);
        String privateKey = "BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B";
        transactionParser.hash(transaction);
        transactionParser.sign(transaction, privateKey, transaction.getSignatureIndex().intValue());
        Assert.assertTrue(transactionParser.isValid(transaction));
    }

    @Test
    public void testSign() {
        Transaction transaction = new Transaction(1527514557052L, "000x00123", addressManager.getDefaultAddress(), BigInteger.valueOf(10L), BigInteger.ZERO,
                Arrays.asList(new TransactionOutput("S2RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCNDV2", BigInteger.valueOf(10L))),
                "424045E72D0DB035D3E41EDFC447765D18167F04FD11AD15656082067F18B5A6",
                "aCQIivOzyZIjDHe7:BcnNZ5Y5tSugpCdUe7Zc::tzgRbkR5mEQvGRXT:gUh5ivo50RPwFXTUW8fU::OR7HBKfDpDhBM+ow:iJ3NqGHkACEt0eZHP9t2::lLzYa5TEFwaismfJhGYr:TiYhAAq8OeMVef4W::P56ACt2Nj9xpRLEycApo:vX9yoZsrOllnTJ69::ZmIBUOYSZAAioGsbvfJl:v7HmywXGRRvblbcN::dB2FXtAWX9VIS6dr:8P2XLoNyjgWZxcKy0RgP::rZKa9yGxpoItIaX5:ncxvVWXM0oclEVSNYhht::mTwbfxoaGPoyq01kOWDI:ofHbSoX1vO+mylOF::RGfC4ZUZiWsm3EIO:tLbdfhxOBAewkN7NUh8Y::8CXCp5Tlgx984sbV:Kl93rzO6pNqInepkPlA4::AvXxoLETij1TQL0e:aP9stFnunePHLnXfntnC::gVIY1OMoviUtLF0p:wveO9vA13j4MLOWDIW7Z::1L8xmHQljIJCoMVMpH5N:PmlESqgMJJ4MtDSD::qHwkEayJNNf0gqketYL9:Lg2URbgyka3pHTmR::Nzja/VRNd30fugyM:x0aLqsFFSPT1VT9qMqAi::jnWQn8qx8b9EzU05xCGe:mZRkUWaUdBlOQ8Q3::5VnTwnUxaoXCEfy0H2Wl:KvCYVHwVHJnaFgY4::V9rg7vuxgOCQal4cMh4e:2YerU0Ml4Pkl77w+::hay0TabZmPjZv4pOScWk:UTHnM7FjQZWP6Vf7::wD4xams/EhSLqpLO:gpkH4GEraMAY66jsJtnq::ZwGR2TOdjiIBN7BKxgwP:xu2yAg24B+HfCM3p::P7qyLyk4lw6maDyPyE7I:9JeCiiVvQXRIfiRS::uj6Z9booLYiJ0KkiPW44:Ur4RYxtHho8xqgMQ::SDUVaGbLDX1iMkkH:Tv4eqcHz8E6ac2aZ3gxu::ilgACh3RzSrAsSHK8Rex:wWC9jUVhEXAShs0M::bn7YLSbWPGQORdxX:KoGuIsSmU9nVNI7IpE4k::lpmxLsybYtpauX8ovdKi:TCQ15QeQlXiSS17l::1Zbev8wTWrliYhOR3DZ4:b5AImQQ1PD1wb5ZT::hAO3+1fEEBxmQY+V:Yv3sq5L53W9QmlrIhu8A::lT0UyytBGsaLYQGk0WCM:krKVxITkBk9nnhag::rwZmPUlFUSrpERNmY6Wb:1zcl5IrpQuNYyf8o::ViD3bzaJ1Dzx4wjn:dV0rVBJWgeFJrEafkcb7::JQeyTMhZlFTtIQOv8Hww:3bKRBjWMQ99SwDpf::H27QNxeHRAK6sapwF3GT:gq9NYJENiMVmgI9m::c8zltxKCdVq3P24I:iDCoyXd7E6s2O9lSS9lB::0uAKZ6A/D1Lo9tbg:hK1e4mYFaKZgOw6v14iq::MqxiC4R0KhLrR6cZ:wsgsBVxpVs5rFgcxXw0L::GyfuRM1oVtElFpik:pw8c09N6BEJ8dN84yFWc::eMeL9zyHrhxOaT7SbzbB:QcCIXaguadmilYso::V44tpXnAEq0axbknK7rG:FO1/fJI+7tUZt1Ng::EZO8wiGRSArYHSoW:gokg0HbJ2I5WIFyhqEnz::HCa9kjFgLFImWZvu:hMZybk2pRnaCg9dPgpqp::2KheFOJ5DGwHtRF7:vliZQ2A4daEfOuOIS10G::dZYhiAjCI5HRZZXEgmvQ:9iknw+E7m7IErbLZ::LiuxpWbgn3FDj5igbWnk:laK7dUiUsmOPin6f::qcTyTSMt3ZuFU7vjpjvM:Lw45Srf2nQFl020L::GJh9oFojunln9YI2xyFe:8/dfuxNmzhzQqDyA::JG9oqun6ACfiQQ70PY0b:11GBTf3jXyTreHVr::tdPDqqUbzfWwScScNIut:NMpSsBf3iPuIMjWf::aZDAX8D4AiykGLqewdJa:ok+tbsMLXE2qbFz2::NzyHwOLj2ehgIhfQ:lQnCejiN3r7RnzgGyidu::p92eIRZ5jyNg1ZPu:OwhCSNViQu9DPr9W6jC5::AmkPZb11oGQvYQLPgAFL:Hz/a6CjJaoJULOmS::utkdsabk3b3ZA/hE:jlKlw0xfApRyaogIBYaH::DzDyheMEAriGVVEQ:MjMzUfPxRza6e4FHKJQZ::ErOk5PFkKqH0OZIN:u8ahAFYgZeAvRR8t8ZNK::BeTC3OcFjqMgSZP4sttz:/HWHS9vCgp4vg5qw::0VI8hgcfgGSZYvQ1:agcQOh5bvENJnUgC1qnz::xyASiWnlw3vnmio363NE:hZjTo7P0V7+S8H/s::j7MwZ66kFb++x5RW:3b06l07W9GPw7lGN3wI4::VRRMdDZDDBeahcUINJXC:VYg3KE50dwzTLgXI::UKr2KUda24cRou7j:GSoDNefcZnbi0I9EikmT::lyatKPyPte7oPF3D:PP3OWXPdsdVZEG21q7UC::7q7oJyqAazXANnc7:RW46xoaA4vCF3H2LzW7r::BCyOr5M30wX7DMN5:dOJFirBN8ubd88ejeEsb::zS2iap6Sg8BY0eCr:Gz5XeQvLUw2wUEjkGFx2::BVqucqtd0qVI091F:7G77xRoHecoUO8biBlYD::d3PBCHJ18zMjAeaW:9y97jFSxBEd5b4rJMwp6::zzWxjMXh8WZsy07vptJd:nadfHGCeOP2tpGuu::FaZRfeIo+v9lCk+b:wWLDgKWh6llODNJIClwY::oQLpCo5cJz0rTR4mtMcH:1iDXEE+mslCGugl8::C7J1OI1n6x3vgWtC:LLfAdpOx8oSY5nJIN7ha::D08VSQ3NrQCmzN0h:gr3wEbXcOKAotTF0amlH::c38OK7MKIfTw2K1i4Ser:vZfSzzQq3YOWQl0G::rmlyg713ntVGrBK3xfn8:8kL4CzDd/OpNklTv::zEOQ5k1XhsCQTACZoqmY:NyWmnN5S/Qh47wHa::gXsPq2JYGDmWlgoC:zHjCAhyGOcXLS65ck1QG::F5Zb/d2AMIXM5b90:SYoLuy1gaAu4w73tf7SY::jXPDrH4PthyUndcO:ZkaZ4HYChBuNVYXrOUvG::ZtP9bDCsd5KH4pbBM4A9:nszbw2y7otrOCuj+::g5+b5Hv9kYugpS/z:YvaNmo4iXtxwW8C6uYDB::zMOuqR0NZZ89JtgwdDgT:1QIAwOe0pXoKfe90::PlLOrQjcgI6EAYxWPQpX:X0eX69xWTW1buzn0::abfxsZgGcuBm3gdmEMBc:qnA9yBtQM4JS8NWK::3/HBlGAFvCTSPIVG:mmOL1oyxHBqY0smNnPXx::P2ROmMKbUHK8EbMB:OOhpKCpkKuLNr1wjztjv::AcNR/+lPtVVK89Zu:Dk0uNBVeY5s2BjzdMrxE::BgMoJ28gkN2Wd7BM:TLvT4RdptzYBH0HuNLhp::iVEvjYUHdPlvYrbt:oKyqKGC4CQrikUyilwiB::7Wf4BycHyaL/fe4M:PkpugbQxIW7zNWgzbA8T::B4oDgPBB1mfjAsrWSgbM:LsfIXffxpI8UpQcR::dND2uMhb+MEQslm3:oucOXhlHCs4Rx21VRzRK::NtHW5tQ/CJb9jBt2:jK0aIEU2egRPCE5mQpMu::xEP33C/G2buG4jWm:tQy5hIPvImpKU0FeFKTX::AbzNRQY83SXS091edBWg:6r+0YXL+VDfnjNrg::YiUPh/6+Fnlyi6f3:R99ecMee2qOOH6JigVsI::iO0eTQhXuYMyAHOPNJih:HoYEWgyg/sLAD+9+::2Jyz9P4hedZHD1ESqVBd:VluZjW3CPlP1+hMc::GQscBlIxthIUEwM4fZdH:OBE7PSyEEHrr80TaahpGP2j59276XjC17bxzYvdY6/M+vAq9iIx3K0/ETHeYSovhszowe6UwwPLb4uQo38sZpg==,+FnrEU1CH4sEV95Iy/wn89OXBFxx51CYUHsErcqIsRo=:GIJx5duN9HWlv7BP+zLAMiZjkm759VFCjvTSvBarL9A=:HTgBVd165hYBJVpvWVsZ4rjSW9PiV+lz4OsxppTGArQ=:HmyeKsmHSjR1CTbLYSNNw8uTcue+k9vE2S3hHg8yIeM=:faH+vt7+2MqVNIK7shnkUvPH7mNcKKmpTgewOwetHlc=:Sa+aEX8UqxGbDBuHXCAAiBTFjASNXX0YNDf8cB81HvE=:qOFk/6O1O9KAYjNcflthbWLn3ZUMdJr+sT3UDUrEzx4=:54YoynhT4hAOBaAAGjworfDHLghToNGtlogbn9LBzGk=:cQpcxcmcExbbGvazSzeRRiJ2k+/smmo5+tmfkopZmRM=:0i4WypLV0+2rmQg+Zrj68CWxZhdKUhmiLFgEIqiFm6k=:qLnYHYKUZoH3lFv1SqQWCMVgdFBNm3LpS0EEactsejM=:LoxlZFbWEdpuU9fpGaXKQM6+lbppPNJGzuks1D7WzNw=:5dOsfZuoTX9hRUs9JPrN5enMRKjWUr+LWonGK7EM8so=",
                0L);
        String privateKey = addressManager.getDefaultPrivateKey();
        transactionParser.sign(transaction, privateKey, transaction.getSignatureIndex().intValue());
        Assert.assertNotNull(transaction.getSignatureData());
        Assert.assertNotEquals("", transaction.getSignatureData());
        Assert.assertTrue(addressUtility.verifyMerkleSignature(transaction.getRawTransactionDataWithHash(), transaction.getSignatureData(), transaction.getInputAddress(), transaction.getSignatureIndex()));
    }

    @Test
    public void verifySerializationWithoutSignature() {
        Transaction transaction = new Transaction(1527514557052L, "000x00123", "S16WKSPIOD2SWPXJFQM4UTQ7P33R4YMMMGGDZP", BigInteger.valueOf(10L), BigInteger.ZERO,
                Arrays.asList(new TransactionOutput("S2RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCNDV2", BigInteger.valueOf(10L))),
                "424045E72D0DB035D3E41EDFC447765D18167F04FD11AD15656082067F18B5A6",
                "",
                0L);
        String before = new String(transactionParser.serializeWithoutSignature(transaction));
        transaction.setSignatureData("signature data");
        transaction.setSignatureIndex(0L);
        String after = new String(transactionParser.serializeWithoutSignature(transaction));
        Assert.assertEquals(before, after);
    }
}