/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.block.genesis;

import io.smilo.commons.block.Block;
import io.smilo.commons.block.BlockParser;
import io.smilo.commons.block.BlockStore;
import io.smilo.commons.block.SmiloChainService;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionOutput;
import io.smilo.commons.block.data.transaction.TransactionParser;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.peer.sport.INetworkState;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class GenesisLoader {

    private static final Logger LOGGER = Logger.getLogger(GenesisLoader.class);

    private final SmiloChainService smiloChain;
    private final BlockParser blockParser;
    private final LedgerManager ledgerManager;
    private final TransactionParser transactionParser;
    private final BlockStore blockStore;
    private final INetworkState networkState;


    /**
     * Too many men
     * There's too many people
     * Making too many problems
     * And not much love to go round
     * Can't you see
     * This is a land of confusion.
     * <p>
     * Now this is the world we live in
     * And these are the hands we're given
     * Use them and let's start trying
     * To make it a place worth fighting for.
     * <p>
     * This is the world we live in
     * And these are the names we're given
     * Stand up and let's start showing
     * Just where our lives are going to.
     * <p>
     * ~ GENESIS
     * https://www.youtube.com/watch?v=QHmH1xQ2Pf4
     */
    public GenesisLoader(SmiloChainService smiloChain,
                         BlockParser blockParser,
                         LedgerManager ledgerManager,
                         TransactionParser transactionParser,
                         BlockStore blockStore,
                         INetworkState networkState) {
        this.smiloChain = smiloChain;
        this.blockParser = blockParser;
        this.ledgerManager = ledgerManager;
        this.transactionParser = transactionParser;
        this.blockStore = blockStore;
        this.networkState = networkState;
        loadGenesis();
    }

    /**
     * Check if there are block in the blockstore
     * If false, load genesis block.
     * If true, add latest block to chain and request blocks from network.
     */
    public Block loadGenesis() {
        String targetAddress = "18C379AC61A573459Dc6E6C2a5aDfFB86fe93a06";
        BigInteger addressBalance = BigInteger.valueOf(200000000L);
        Block genesis = new Block();
        if (blockStore.blockInBlockStoreAvailable()) {
            LOGGER.info("Loading block from DB...");
            Block latestBlock = blockStore.getLatestBlockFromStore();
            if (blockParser.isValid(latestBlock)) {
                smiloChain.createInitialChain(latestBlock);
                networkState.updateCatchupMode();
            } else {
                LOGGER.error("BLOCK " + latestBlock + " NOT VALID BUT IN DB!");
            }
            Block lastBlock = blockStore.getLastBlock();
            if(lastBlock == null){
                LOGGER.error("Could not get last block from longest chain! ");
            }
        } else {
            LOGGER.info("Loading GENESIS block...");

            // Create the beautiful genesis block
            genesis.setTimestamp(1530261926L);
            genesis.setBlockNum(0);
            genesis.setPreviousBlockHash("0000000000000000000000000000000000000000000000000000000000000000");
            genesis.setRedeemAddress(targetAddress);
            genesis.setLedgerHash("0000000000000000000000000000000000000000000000000000000000000000");
            List<Transaction> transactions = new ArrayList<>();
            Transaction firstTransaction = new Transaction(1514764800000L, "000x00123", targetAddress, BigInteger.ZERO, BigInteger.ZERO,
                    Arrays.asList(new TransactionOutput(targetAddress, addressBalance)),
                    "",
                    "",
                    "Rl9uobWXkoR9jL/D:pvPnASeNNlSHoS9q3cXm::BLEMeJLndBsAWSbh:W8ix8xGCm4khCLAWTvwr::F3Nw25RqSA9YMZUa:K84sRwBUzLK5iKVKKRME::ben1f7jdtx7yuRbnMT4Z:rOpg3hXLsoOPKAOJ::Pn6z7SUbeX5rx/Qj:dXCHlnfutBXGhQfbhXsU::EEZXnzOwxmZ0Yv7CGUDl:UaYcmHWyKmivIruG::PV4fxbLq1yaeVkB8w6p5:pXNMuB2p6HxEhgjI::yb6eXfPhgk362YX/:j2bDpMXXHZkyavbB2MQN::k3rH4Cly8Wkd8FqN:pWE0B5vnoUw2VduGbIHE::klM9263wdee9pTygUk8V:tFKJ9w3WBPIslFH5::nK744uk4LasQcHRp:6t99iFPGd5y3WiYW12dy::WlY4eJ1quxcoyG8YDb0b:yutIXxcFHAVnAeMw::NjhEHsKKubanW9ATklez:Wqey3QLGk2TXLe8t::BJ8bzMtExwke9wiWxCTG:E2WOg2/GZFA5Rtma::E6kwEijDtyyd1C4p47dv:NezvvZrFsccsudHS::ekn9Fxeg/fyxReKm:DcgGv9oqjZDra6Zhb43x::twwAca6tB96Hvhf/:WeRbc5OadfQtifvSB5aR::Zc/sSKHiN/aJLGdO:Zx6tuh07o7hi2lYNXNgC::gUMwbMzYVK25FLiTF4k2:PdHDPaoywMrjzQ+z::cna9I1qwl2i89CW1:oybCx7leLa9tG8LZBlrq::xK45TM8pM8NIYkanl59B:P8dpjFLLZKew4c8X::3vKk7mMjv6kAhECD:ig07Wmm3vI28AWwa1lal::2AmGYKujqZREVUtOKnJp:2yAnSwwLbAkJvbrn::VVTE1g6gdxxJgccD:WJmwDEDF0AtPdNiUxzXW::skCnuK48pTE4nUZU8Vzx:y2rW0PPQEdZpSHex::y4bkRlbbJYQXvclz:xCT41uctREN4ih83pwJT::eLYdflpzTBGnr1gtKp2t:kzlreZk0acOZ85GY::4yDA/H86bjaEmRtf:jp5EblCFR1jUHzUdoq09::RFCT0JHiePzZdJS8:OCJB487OleqXv4DRXCU8::l73HWnRvlxEjGdLhCVU6:wA/EaYbODK5ZQ52T::0SxJZGOunMDTQ2VF:ngSnwyVSustkHYhjLVTH::VANLrt1wGVGPdmrLX7N6:DC7mAgsE+hjyAOGW::RQczfdblYkZq3yYPtjpg:BpOE/sS1D+v8vTiZ::PFJJ2Qb5knBVNy6Ad2mU:x2OI2VTi7t590auE::lOTIHXQ8pTLudPPN:MdRGA61Ldw8GHju2EY5d::LoWtB8oxkzJhLgCRpCf9:3KCf3KzE575vqzAP::01lt7doSuBi83d20F9E9:qsos2awhSEhRC5ir::teYMpFNp8qNk4KD6:GZRvadeWzDKWwcwyMSxG::y0XInQcPApZN4kDyRMXY:ZPRGZatmWJwAywom::TTEFhq6YJSuB75mF:XmSBy4C8ynZgNr73dJmz::Nt40LJXUEKW1vr9nDGYb:G/R9d20o54cHkvdK::UiktK87yfsiynAgfa5Hh:h1uW6AudVCo5GKve::rXVsd9pM7rKybi13RCGj:aHk9gSIJmXWQlHPC::V6zVS7w0TWSIJoDt:IVvoyRIytCJ9F6wcIfoP::kKL5KqcnqZhDhoNh:0hLH3K7LslWWs8STuGyw::z3LI3RAfW49lbvIgpsvK:YXeSPj9j4fE3mscd::4WrXR5kckg+NaA+8:daYUzG995VJVpb7TrIxB::JunALdexLCMcFZNa:RoRAhaKiLMwzDvsVr680::L8/+T9bOGhyl1byb:Ap2EV4qvDyLuAT0IXPKL::o7U94UtY9StDbpvDSg1A:Y045Ett8+Na4+fZf::wE2yzuhyGIGFQsC8zDAZ:mCteKkJ+R6rt43dZ::hb9rSVy3pTRsRhlj:rdprp1H7C7T6Q34j3HcQ::5SWiDnzJ2EbvSdfAbYfD:Tec6gf212QSJTnVs::Ji0SqLoKk7YhPa8xqzLV:NnyXwJilbS7jxVnq::ECBRzTf4JYGrfWWD:8k1n0MaBu9wOaued6deV::7L9FyGU2LOHeAXLx:Zu6wyMQu1RklOyEtMwWV::32zqkUwAqxLk0cSr:pSvQdZ70iAo5elb5xAuS::3k3HSLxLjvXmGToG:NRJmVXgD3wneC1MK8rVM::s3HdiWmbSG6S43H0:m8B53FAUVkOFW1bmoMOp::Ra7ssZneXU8wkw6J1GYn:w72SGc9wHMUY6WIv::JMdq8ljyy3dWMDHOvjRV:6hsu6YsZXtmPQ5in::VfhoaytHRzONUdLkeI0r:w2rDnWA8nVcbcXRU::ixujiOWk0NV9tiEPenhy:7p/2SQpZ9wrSZfDq::DtJJIXdy5K3OHSIg:q1oLrCsI9iVFXiiDaldu::nzSuWNj6RvaUUltc:1xzULV7BuRK5YjX0UZls::6HSzC0EesL2Wx4REqtzz:1gUwiMLUEP6OmS89::W6v0oGFueeLumLEXD1lp:uNR93nFp6M1SMNp8::iexotfZ7YSzIyFl6O31r:rZRuSF7/3SyKcJM/::0NMY4afUffqjG71e9qW7:9iwTDn5qs/d9FDZL::HKTHOHqZTACit2tP:glM16ApBu8BUEux2Xmbe::9phu0ghj55q44xX29gzk:Yts4bIfW3/+u6nvA::7k5Xjtb6KCTqJICyVV5A:bcWAa9/KLSDezwoz::j6LGoT6Qi5W4OJS58t2Q:aZ+PFvsIWULtpKAh::/vfFxu1ze0WVEgjQ:dwe2m2dRKFt8OCEL0Tv9::vjvvfx0g4e5sSklpx5bd:B9Ei9eHl4wfV5vwe::YPZu5Z+NwByghh4B:LTN6LjkguqDc3NEIV433::d3sZDskFkFNNj0fu:3vx4VcA2yY49rjcw4IBw::9JYz9BBAam/+lEyG:Cft94ztQDhjbieOA0ICy::BFbNUeOHQWkyznVnsaYd:uBGjPHxdl5nSkI5Q::7sprLO3DiZt8UnLUJEP3:mn4/ek2Uz9A1pnjq::VEwqno8WpMJGAucyC8yP:6p69QkabZ0yU3Q7h::Ecd33gie1DgW2Ncg:Yr9NrGF2MAaZ0c6ZWITL::EGXcNUrQr0LVe63B:vm9ROrqeTByPwMMS5ASF::IB6UJLBnrv3m3IzI:wjJr9VMQvfRvtUwk84rX::3TP8Pa42KDtl1uhcVMgr:x3R/T2ZYG1OiWQEe::HAPNvWHDgQwutp11LnCp:Pmlhfx5BntDY6BLv::rnRChNqXrwXLBb36:PAC3VJuq15LL9hLT8auD::T5vnEZbT0eWjI433:rvz0WRsZ2Ud1hWEduhJ4::21ADl+WGq8Vk92lg:RETVYXx6LztWbPHOZR7Q::Iazkg0bCyceGuoad:x5xTgNKN0sx87FeuVyjP::zpEFMyjJxUDUrA8G:JWwqxiuPYYwrG3aYdJ4n::Wn3jMYTY/hHUbbrI:cR4Tkm1bsS3YWqcd9I1l::OV2RsE9cz8LQMTQA:QulezeCRpKvwsNZXIjy7::JmUasYo/gA2sQQ4L:X88Eq5iDE1ihXZp6mu4W::hhe4vxIbFBjng0QWL6pJ:fgu6mo5zmgnbYz3R::i3EMrIcvEqz0shTK:YEnJM8SCWfuEi7Y0ybEs::hdIh9R9Itw4CdkoM:aZHaK7a3yrLGLgvuWANP::bjFLMsL+1V1NyrMH:BcqOwm3nAVQLUIcFy0gM::Jz4SJGu995Pyexq5:iqFARP5EVzZHbUoyESOL::YMY8W5ytAL2XsfUWGYWk:VsKmwDCK4UbhBZilo81e5YuY+B9T9VAec+nTwYFkTrUNhnevjRjtWd97IF5rfKLA3yA6UjHE3ax1/Y96DELqCQ==,SNN7701UJc0fqXfdJ6pnV8nS7zTEh9BCWolm7PYj1Dk=:B5FvaU6rC5LZFlz53lDH4JjSMrUgro0yy0JSingBTnA=:oOncFkcMwQmz16hY4vdPYvoEhQvcnQIb2+bikp/n9TQ=",
                    0L);
            transactionParser.hash(firstTransaction);
            transactions.add(firstTransaction);
            genesis.setTransactions(transactions);

            try {
                blockParser.hash(genesis);
                genesis.setNodeSignature("FSPWKgck9EV1dD5E:Iz9Z5uHsmskJNWltZUL5::tNoUj1c71InYEiUSq6fA:PC2N5g8JjMD6jQm0::AgQX7xQHHeG4quUn:huJeZaJ17v1oUlsGM9uv::eoDau70Su3tG6dA6:yseJXkIFF9aniMoiuy9G::IQMUzKYuGM12gEhi:pqUZisIcN1v9nVS84p2L::1WZb9IY656D9Ky1s:AYKgLT3L5mU1KnXFsxJq::lej2ltc5RPnjsHG/:YoV6TD0dUWj5Si97faXg::OCFgH7SGBdlmYnME:z6msqPo3FuJe8c89XiaM::UEyMN8RgbghwierzizNI:OE2rinAWqmFYSpvl::tFY73u34OBsiG7CVsz0X:+otqEXbE3eCTnl3+::6b2MtondjDwXChJg7xFd:KC1SXuBDH9bv07IS::XZQGy37rrDDMQYewMKwi:3FdOHkgdbCrDsb4S::vm2udD8pvXrDKUtnNNZi:7whjw4VOXi4KPiUi::v08mSbkttDz2SrkB09Pu:X3xEr+m/XzihH3iH::jxkomBHSG9pEbmDZgDaZ:iWyU77iLtyOp9ZSi::hAh9rNIl0sMNL/Rv:LYpWOMy5J9NAcgQuMVsj::y6K73JsoaCHe3V1KWutw:VEeB3MFHycmjWige::mSZDyPKV6P1V1509:tWxV0VcJDrRAqdygOxQ1::RpMpaH39VCrLOIB2fhgg:M9tjEpoOt4UXcX9J::6QDcCefar83PlXxabHEm:RcN++/aM9jTVNN+N::TiQUDC4kQdTLxyfALf67:+tgo092IHZsrTod4::rLxtEbRoOElK8DxSYkiz:Tw5ibF2XOpbiIGcc::iPby581RWjKDJ7Sr:g2zD3Bwy89WAZHxaRBDS::D5sJSHuR5cWAzSDg:XQqE1QYeaZR3QI1a9Cah::cDetRAzMqtsS7fxf3JXt:+NnWnJhU/xZ9Axgr::uynzUdECtB9jSVZf:NpgXlSwgTwQaE0RuNlrF::duy5gbvdrfanmcPT0pdF:2z2n4Mehb+iwhnEl::TV07hhZa2Kf9b3AS:ENu8MN71TSsdmNqHOwlM::7QrK4Vu6Dc5iDOirLvf5:p3c+lcGQGrKOWX4f::exOnaAyDme3fZtQUHIaO:hk2DkzaK/5P3+ZC6::NDGxB0a8zrBhMuh1SCkz:BYZiyt/a9JL0Dp0f::tVqvENOEjOFsP9lB:LSRUAOZULwf3lQrAT079::b9Dv3WWkMNNETyzid0DQ:yz4MBYf5ad+ot7gJ::i1JdnVkgzFuhBltNaHkP:2/vfWgOEbbUEVbok::ox0cOvLdbZ5DI+R8:m58yYbwOLifjv2j2c39c::WtDWaZOsvYjBZjOSJfpQ:aJTYsX4crSKH89f0::yEfSYIfFVtnIh2OwlUSo:UbOArjIN9DjD7+4O::dLqJT1UZOCwpqDLitqLU:7s1xZHx1Z1XaYfU5::ss9aoq7SJB8jCv05RYcb:xDJmgVAefN+mDFOR::KfzHVyw1sHTQ0wQ8peIj:SO9kc6HJTYEwdNIH::P1PwcEaeFZL9f2VMMkJG:vAkBo7+rUDSlixj9::aX0IulRwxDdiR91e:Fv9znAJIFnbx0twfzfdK::NLYrSnlhe9gxcoPV:rwYNWMmuFq48qpXfHGau::g2dqCaNJmHYH/R16:f3g2iwgRtrPkXrv8eoTt::BRpOyPXeURei4Oavu2FS:bZR0oHPtu9Bo9RwU::sUBH5VmMcT3N5o8Hp3Lb:D3o+T7eJ36Kds6tG::49kB7DPKmLOdO4j1BJ3a:NOCYm3fnp85pXAEB::tHKA7zszoEuneyGuTopA:nHSAX9TkDl36lWCK::8RG6xI9z9jNZ7HQMHt75:i+IZVDDaFH4zu4X9::uGAtoQGudgyiLKse:2GNOtSlIHRFLTjTf4Pav::drzipnSk5lxWeKOA:8nnpjyTbmo8tySGBw2Sm::tAyACD69sMiVFgeEVe16:CGYNuDP8zFSMs2Ga::YZN4Vir+t6qWyBgy:3Sf5BHtXTeJJMfZ3Cgos::e8CJReDGWckroQb9:BuFR4Sn54fvkAYVRYIeQ::xZyptEhlvpHKHuBfut21:z7L0zeq+4URfZGHr::EVFW7tRqKPKzYGwplBMH:rH6eotz0KQOrMLAy::WnWrg3el2ojB17Ok:kWKX1btPl9xNejvYe9NX::XCmVssvuTrnrdh4yOGEa:N/T8qwXaRvBn/f+6::1xXcvYGLKxXXpiXEEKeA:SZhP10RJ/506qiNK::RW0Nhtip0Lj3KOKhNXKy:128Sd+40r8H8UMvw::bfk0MAJQAZLVmEHB:2Pd5C3No6Oa7SLe8Ba7d::5mOye0FLPg1bMA1a:H6O3wyPKK4p3As8rOlMG::yy+MWAPsVxitzjqt:MNmHuoGtwxCP8uUMAhaI::KFLEJLqVJG3sNYHsDUAj:+UXlIhbaXwZXdfMZ::4eoDnkZY46Wjgy62:yDlQpka9GzYI2s5SpMXG::Z1O8T5ebleTvb58i:enbErvrMBAONaUuYXfti::40XyM3UUGY5HZhVUldnw:ENWSBu5lQw8ZIeep::IFD1JeAxxSLWf9of:tj3NYJFrPvkgw6qHsclc::LfXLxfFeGmnkmPKAymKZ:OiRPppxc4QxADDG4::FqLk92ZIaHOlov2b:Ih9mClbSBIcLYrrDyby7::8oYzmo3scANQiPcGiDVz:Li/W0xNvOi0Hp65z::cC8YhsqIBTmQgCyE:btqiynKTpoCcQECLjWGW::EiVIiWtmX5uxhgJH:8lrbFJSmCOiWkMPBHOQo::ZT9n2jA6gZaNuMMi:znXoJslaUoqJR2VShNPm::4XSO1Dzw5Df1YdyA:0TuO761pu3SqfT4nG7CW::qs79DMeTMMnWO39o07OL:d/L9XgTB3WFbe8C7::gg5E0I702Z62O4aR6NmA:mmcATUxZwZP4QZwC::l3zYqeo2x0e4Bv5i:9JlKeNhTn5su6ZYPHxDK::MYxqaqM8WsMH/jUT:k3OfyG85q2ZL7H5SXas0::T9ePm2VUEhCsgYdO:0GHqoWkbUmr8m2TLIOH6::zPw9eyKSCr61eVdtRuaT:nL4AWv64u8snQz3t::TdB8gW5bdFVNcsjKHm1p:/uOX2LmZs7BkwJwp::XLwu4XBQGH0c0vJmhgM0:Ijx5Hnl/zsvgwLS3::V6OaNqn4ZIpj9fhz5npa:iE+L+hTHQIv6Jvbb::RKiypqgJ3zd/ljx/:AYXQdOC3jy8HRxOSkfpF::9JaHkFa+MRV7GrKO:3HOaSlnMBzJe82rQtlah::0yQhowBfKxS3cLX3:NXSxQLRVOex69GuH3jzR::m0y3pb+fpMyEK2vq:Rukb5bRy9lmCAuoy72sK::s0Jbfipf3Swb5Bz7:gcAIP9KXsmi6BAWnkTvt::xtjKEsMhXo8oCu5L0yrq:BtS9qAQSAfcZUn+h::SSrYNuM9XEFbofpF6bLu:sJx1P3fttEImAMOe::1Aq4P933jfy2MevQmdIh:7GkFTB0SYy92hihA::GDGNWyDhTabYjVbf:g2cqIIjRwmXBX5vHCrCv::IpVSPFmASki0gYkj:8APtY67FHCP9QJk4SLL2::CqamwT7vK9zeXRvFNfY4:WirILL2a+GSFBHxl::+AYuIdMPq8GiQ515:HFRG5DkLyAKLx6ajmKWA::BocCOxYC42YOC93WRDYD:UDc8BnL3qlJi9hCJ::ajFkgJdy0ZqSM7PJ:tfvUYSnyqFmilf0vOhVi::iRsupbA9ZkoTeVUUzBuf:EBz1AB01uwkAhX7S::MqeFW1heYo0m85sampd9:g9cD1y9w1HgdCvXe/SdLCENQJfG1DkgG5Xjv3Fsygu5yoq7G+HvNcECYQ1hSDMNKv9zw+8St1YS6/cLuPgaqkA==,2LJjoi3AHhVmo0bkFrrBQnnVaZgLCd4c3SMVMz9URYg=:aJi33m6RWEyBTlTJ0FX2NdIOv5JyYLvBUBfgLMmagUc=:JSXFxf/y9QA5N2GSMi2VRx6myIYTMVcBFBDWiSWOzdU=:BOzsi7Rf7n42OlfKhdl2hdjikhX/ZnQFeXHum4SvgMQ=:O+/1A+Z0rDGVgrp3ZFux2itCRwMi2FPzcS2v6Znd9X8=:SMxRsen0ZcNOOQOEga9VHWlh/Y5VXPBTgXHMFl6W0Jw=:urrs+BBjLeio8dwYYuRxgC7b6tHpBMnIQmk0t5nD9Nc=:voOyTXZi5zMFRRlijSY7M4atrD1rqXFBy6CgjR5I48c=:BVrK/BStKRpQCUIPBOM5Axw8sqcK/2wYh7/p/YGXZjY=:Y6i3qIxq22qFHFQtF6yPNfnzlRhwUCpyESu+X/v6mZg=:Zx3tQR6D4nW6BbxKXf4P6MGXHqcnzL8qREMFNJT7Cuk=:OXWXgApfUdT1Ve73POpjsjp44R6LNHBiGgSZOj5c+8E=:XezpPlGSH7ph5casnXCJEqmYkUjOPOVAItfqv3NbHAM=");
                smiloChain.createInitialChain(genesis);
            } catch (Exception e) {
                LOGGER.error("The GENESIS has failed!", e);
                return null;
            }
            boolean updated = ledgerManager.updateAddressBalance(targetAddress, addressBalance);
            if (updated) {
                LOGGER.debug("The ledgerManager has updated address with balance " + targetAddress + " , addressBalance: " + addressBalance);
            } else {
                LOGGER.error("The ledgerManager has failed to updateAddressBalance " + targetAddress);
            }

            BigInteger balance = ledgerManager.getAddressBalance(targetAddress);
            if (!balance.equals(addressBalance)) {
                LOGGER.error("The ledgerManager has failed to double check address balance ? WTF!  expected: " + addressBalance + ", actual: " + balance);
            }


        }
        LOGGER.info("SmiloChain initialised ...");


        return genesis;
    }
}
