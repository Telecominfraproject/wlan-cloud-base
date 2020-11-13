package com.telecominfraproject.wlan.core.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class CountryCode implements EnumWithId {

    private static final Logger LOG = LoggerFactory.getLogger(CountryCode.class);

    private static Object lock = new Object();
    private static final Map<Integer, CountryCode> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, CountryCode> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    public static final CountryCode AD = new CountryCode(1, "AD"),
            AE = new CountryCode(2, "AE"),
            AF = new CountryCode(3, "AF"),
            AG = new CountryCode(4, "AG"),
            AI = new CountryCode(5, "AI"),
            AL = new CountryCode(6, "AL"),
            AM = new CountryCode(7, "AM"),
            AO = new CountryCode(8, "AO"),
            AQ = new CountryCode(9, "AQ"),
            AR = new CountryCode(10, "AR"),
            AS = new CountryCode(11, "AS"),
            AT = new CountryCode(12, "AT"),
            AU = new CountryCode(13, "AU"),
            AW = new CountryCode(14, "AW"),
            AX = new CountryCode(15, "AX"),
            AZ = new CountryCode(16, "AZ"),
            BA = new CountryCode(17, "BA"),
            BB = new CountryCode(18, "BB"),
            BD = new CountryCode(19, "BD"),
            BE = new CountryCode(20, "BE"),
            BF = new CountryCode(21, "BF"),
            BG = new CountryCode(22, "BG"),
            BH = new CountryCode(23, "BH"),
            BI = new CountryCode(24, "BI"),
            BJ = new CountryCode(25, "BJ"),
            BL = new CountryCode(26, "BL"),
            BM = new CountryCode(27, "BM"),
            BN = new CountryCode(28, "BN"),
            BO = new CountryCode(29, "BO"),
            BQ = new CountryCode(30, "BQ"),
            BR = new CountryCode(31, "BR"),
            BS = new CountryCode(32, "BS"),
            BT = new CountryCode(33, "BT"),
            BV = new CountryCode(34, "BV"),
            BW = new CountryCode(35, "BW"),
            BY = new CountryCode(36, "BY"),
            BZ = new CountryCode(37, "BZ"),
            CA = new CountryCode(38, "CA"),
            CC = new CountryCode(39, "CC"),
            CD = new CountryCode(40, "CD"),
            CF = new CountryCode(41, "CF"),
            CG = new CountryCode(42, "CG"),
            CH = new CountryCode(43, "CH"),
            CI = new CountryCode(44, "CI"),
            CK = new CountryCode(45, "CK"),
            CL = new CountryCode(46, "CL"),
            CM = new CountryCode(47, "CM"),
            CN = new CountryCode(48, "CN"),
            CO = new CountryCode(49, "CO"),
            CR = new CountryCode(50, "CR"),
            CU = new CountryCode(51, "CU"),
            CV = new CountryCode(52, "CV"),
            CW = new CountryCode(53, "CW"),
            CX = new CountryCode(54, "CX"),
            CY = new CountryCode(55, "CY"),
            CZ = new CountryCode(56, "CZ"),
            DE = new CountryCode(57, "DE"),
            DJ = new CountryCode(58, "DJ"),
            DK = new CountryCode(59, "DK"),
            DM = new CountryCode(60, "DM"),
            DO = new CountryCode(61, "DO"),
            DZ = new CountryCode(62, "DZ"),
            EC = new CountryCode(63, "EC"),
            EE = new CountryCode(64, "EE"),
            EG = new CountryCode(65, "EG"),
            EH = new CountryCode(66, "EH"),
            ER = new CountryCode(67, "ER"),
            ES = new CountryCode(68, "ES"),
            ET = new CountryCode(69, "ET"),
            FI = new CountryCode(70, "FI"),
            FJ = new CountryCode(71, "FJ"),
            FK = new CountryCode(72, "FK"),
            FM = new CountryCode(73, "FM"),
            FO = new CountryCode(74, "FO"),
            FR = new CountryCode(75, "FR"),
            GA = new CountryCode(76, "GA"),
            GB = new CountryCode(77, "GB"),
            GD = new CountryCode(78, "GD"),
            GE = new CountryCode(79, "GE"),
            GF = new CountryCode(80, "GF"),
            GG = new CountryCode(81, "GG"),
            GH = new CountryCode(82, "GH"),
            GI = new CountryCode(83, "GI"),
            GL = new CountryCode(84, "GL"),
            GM = new CountryCode(85, "GM"),
            GN = new CountryCode(86, "GN"),
            GP = new CountryCode(87, "GP"),
            GQ = new CountryCode(88, "GQ"),
            GR = new CountryCode(89, "GR"),
            GS = new CountryCode(90, "GS"),
            GT = new CountryCode(91, "GT"),
            GU = new CountryCode(92, "GU"),
            GW = new CountryCode(93, "GW"),
            GY = new CountryCode(94, "GY"),
            HK = new CountryCode(95, "HK"),
            HM = new CountryCode(96, "HM"),
            HN = new CountryCode(97, "HN"),
            HR = new CountryCode(98, "HR"),
            HT = new CountryCode(99, "HT"),
            HU = new CountryCode(100, "HU"),
            ID = new CountryCode(101, "ID"),
            IE = new CountryCode(102, "IE"),
            IL = new CountryCode(103, "IL"),
            IM = new CountryCode(104, "IM"),
            IN = new CountryCode(105, "IN"),
            IO = new CountryCode(106, "IO"),
            IQ = new CountryCode(107, "IQ"),
            IR = new CountryCode(108, "IR"),
            IS = new CountryCode(109, "IS"),
            IT = new CountryCode(110, "IT"),
            JE = new CountryCode(111, "JE"),
            JM = new CountryCode(112, "JM"),
            JO = new CountryCode(113, "JO"),
            JP = new CountryCode(114, "JP"),
            KE = new CountryCode(115, "KE"),
            KG = new CountryCode(116, "KG"),
            KH = new CountryCode(117, "KH"),
            KI = new CountryCode(118, "KI"),
            KM = new CountryCode(119, "KM"),
            KN = new CountryCode(120, "KN"),
            KP = new CountryCode(121, "KP"),
            KR = new CountryCode(122, "KR"),
            KW = new CountryCode(123, "KW"),
            KY = new CountryCode(124, "KY"),
            KZ = new CountryCode(125, "KZ"),
            LA = new CountryCode(126, "LA"),
            LB = new CountryCode(127, "LB"),
            LC = new CountryCode(128, "LC"),
            LI = new CountryCode(129, "LI"),
            LK = new CountryCode(130, "LK"),
            LR = new CountryCode(131, "LR"),
            LS = new CountryCode(132, "LS"),
            LT = new CountryCode(133, "LT"),
            LU = new CountryCode(134, "LU"),
            LV = new CountryCode(135, "LV"),
            LY = new CountryCode(136, "LY"),
            MA = new CountryCode(137, "MA"),
            MC = new CountryCode(138, "MC"),
            MD = new CountryCode(139, "MD"),
            ME = new CountryCode(140, "ME"),
            MF = new CountryCode(141, "MF"),
            MG = new CountryCode(142, "MG"),
            MH = new CountryCode(143, "MH"),
            MK = new CountryCode(144, "MK"),
            ML = new CountryCode(145, "ML"),
            MM = new CountryCode(146, "MM"),
            MN = new CountryCode(147, "MN"),
            MO = new CountryCode(148, "MO"),
            MP = new CountryCode(149, "MP"),
            MQ = new CountryCode(150, "MQ"),
            MR = new CountryCode(151, "MR"),
            MS = new CountryCode(152, "MS"),
            MT = new CountryCode(153, "MT"),
            MU = new CountryCode(154, "MU"),
            MV = new CountryCode(155, "MV"),
            MW = new CountryCode(156, "MW"),
            MX = new CountryCode(157, "MX"),
            MY = new CountryCode(158, "MY"),
            MZ = new CountryCode(159, "MZ"),
            NA = new CountryCode(160, "NA"),
            NC = new CountryCode(161, "NC"),
            NE = new CountryCode(162, "NE"),
            NF = new CountryCode(163, "NF"),
            NG = new CountryCode(164, "NG"),
            NI = new CountryCode(165, "NI"),
            NL = new CountryCode(166, "NL"),
            NO = new CountryCode(167, "NO"),
            NP = new CountryCode(168, "NP"),
            NR = new CountryCode(169, "NR"),
            NU = new CountryCode(170, "NU"),
            NZ = new CountryCode(171, "NZ"),
            OM = new CountryCode(172, "OM"),
            PA = new CountryCode(173, "PA"),
            PE = new CountryCode(174, "PE"),
            PF = new CountryCode(175, "PF"),
            PG = new CountryCode(176, "PG"),
            PH = new CountryCode(177, "PH"),
            PK = new CountryCode(178, "PK"),
            PL = new CountryCode(179, "PL"),
            PM = new CountryCode(180, "PM"),
            PN = new CountryCode(181, "PN"),
            PR = new CountryCode(182, "PR"),
            PS = new CountryCode(183, "PS"),
            PT = new CountryCode(184, "PT"),
            PW = new CountryCode(185, "PW"),
            PY = new CountryCode(186, "PY"),
            QA = new CountryCode(187, "QA"),
            RE = new CountryCode(188, "RE"),
            RO = new CountryCode(189, "RO"),
            RS = new CountryCode(190, "RS"),
            RU = new CountryCode(191, "RU"),
            RW = new CountryCode(192, "RW"),
            SA = new CountryCode(193, "SA"),
            SB = new CountryCode(194, "SB"),
            SC = new CountryCode(195, "SC"),
            SD = new CountryCode(196, "SD"),
            SE = new CountryCode(197, "SE"),
            SG = new CountryCode(198, "SG"),
            SH = new CountryCode(199, "SH"),
            SI = new CountryCode(200, "SI"),
            SJ = new CountryCode(201, "SJ"),
            SK = new CountryCode(202, "SK"),
            SL = new CountryCode(203, "SL"),
            SM = new CountryCode(204, "SM"),
            SN = new CountryCode(205, "SN"),
            SO = new CountryCode(206, "SO"),
            SR = new CountryCode(207, "SR"),
            SS = new CountryCode(208, "SS"),
            ST = new CountryCode(209, "ST"),
            SV = new CountryCode(210, "SV"),
            SX = new CountryCode(211, "SX"),
            SY = new CountryCode(212, "SY"),
            SZ = new CountryCode(213, "SZ"),
            TC = new CountryCode(214, "TC"),
            TD = new CountryCode(215, "TD"),
            TF = new CountryCode(216, "TF"),
            TG = new CountryCode(217, "TG"),
            TH = new CountryCode(218, "TH"),
            TJ = new CountryCode(219, "TJ"),
            TK = new CountryCode(220, "TK"),
            TL = new CountryCode(221, "TL"),
            TM = new CountryCode(222, "TM"),
            TN = new CountryCode(223, "TN"),
            TO = new CountryCode(224, "TO"),
            TR = new CountryCode(225, "TR"),
            TT = new CountryCode(226, "TT"),
            TV = new CountryCode(227, "TV"),
            TW = new CountryCode(228, "TW"),
            TZ = new CountryCode(229, "TZ"),
            UA = new CountryCode(230, "UA"),
            UG = new CountryCode(231, "UG"),
            UM = new CountryCode(232, "UM"),
            US = new CountryCode(233, "US"),
            UY = new CountryCode(234, "UY"),
            UZ = new CountryCode(235, "UZ"),
            VA = new CountryCode(236, "VA"),
            VC = new CountryCode(237, "VC"),
            VE = new CountryCode(238, "VE"),
            VG = new CountryCode(239, "VG"),
            VI = new CountryCode(240, "VI"),
            VN = new CountryCode(241, "VN"),
            VU = new CountryCode(242, "VU"),
            WF = new CountryCode(243, "WF"),
            WS = new CountryCode(244, "WS"),
            YE = new CountryCode(245, "YE"),
            YT = new CountryCode(246, "YT"),
            ZA = new CountryCode(247, "ZA"),
            ZM = new CountryCode(248, "ZM"),
            ZW = new CountryCode(249, "ZW"),

            UNSUPPORTED = new CountryCode(-1, "UNSUPPORTED");

    static {
        //try to load all the subclasses explicitly - to avoid timing issues when items coming from subclasses may be registered some time later, after the parent class is loaded 
        Set<Class<? extends CountryCode>> subclasses = BaseJsonModel.getReflections().getSubTypesOf(CountryCode.class);
        for(Class<?> cls: subclasses) {
            try {
                Class.forName(cls.getName());
            } catch (ClassNotFoundException e) {
                LOG.warn("Cannot load class {} : {}", cls.getName(), e);
            }
        }
    }  

    private final int id;
    private final String name;
    
    protected CountryCode(int id, String name) {
        synchronized(lock) {
            
            LOG.debug("Registering CountryCode by {} : {}", this.getClass().getSimpleName(), name);

            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if(s.getName().equals(name)) {
                    throw new IllegalStateException("CountryCode item for "+ name + " is already defined, cannot have more than one of them");
                }                
            });
    
            if(ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("CountryCode item "+ name + "("+id+") is already defined, cannot have more than one of them");
            }
    
            ELEMENTS.put(id, this);
            ELEMENTS_BY_NAME.put(name, this);
        }
    }
    
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public static CountryCode getById(int enumId){
        return ELEMENTS.get(enumId);
    }
    
    @JsonCreator
    public static CountryCode getByName(String value) {
        CountryCode ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }
        
        return ret;
    }


    public static List<CountryCode> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }
    
    public static boolean isUnsupported(CountryCode value) {
        return (UNSUPPORTED.equals(value));
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CountryCode)) {
            return false;
        }
        CountryCode other = (CountryCode) obj;
        return id == other.id;
    }   

    @Override
    public String toString() {
        return name;
    }

}
