package org.openhab.binding.bluetooth.daly.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;

public class Daly extends KaitaiStruct {
    public static Daly fromFile(String fileName) throws IOException {
        return new Daly(new ByteBufferKaitaiStream(fileName));
    }

    public Daly(KaitaiStream _io) {
        this(_io, null, null);
    }

    public Daly(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public Daly(KaitaiStream _io, KaitaiStruct _parent, Daly _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }

    private void _read() {
        this.frame = new ArrayList<Frame>();
        {
            int i = 0;
            while (!this._io.isEof()) {
                this.frame.add(new Frame(this._io, this, _root));
                i++;
            }
        }
    }

    public static class MinMaxTemperature extends KaitaiStruct {
        public static MinMaxTemperature fromFile(String fileName) throws IOException {
            return new MinMaxTemperature(new ByteBufferKaitaiStream(fileName));
        }

        public MinMaxTemperature(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MinMaxTemperature(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public MinMaxTemperature(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.maxTemperatureRaw = this._io.readU1();
            this.maxCellNo = this._io.readU1();
            this.minTemperatureRaw = this._io.readU1();
            this.minCellNo = this._io.readU1();
        }

        private Integer maxTemperatureCelsius;

        public Integer maxTemperatureCelsius() {
            if (this.maxTemperatureCelsius != null) {
                return this.maxTemperatureCelsius;
            }
            int _tmp = ((maxTemperatureRaw() - 40));
            this.maxTemperatureCelsius = _tmp;
            return this.maxTemperatureCelsius;
        }

        private Integer minTemperatureCelsius;

        public Integer minTemperatureCelsius() {
            if (this.minTemperatureCelsius != null) {
                return this.minTemperatureCelsius;
            }
            int _tmp = ((minTemperatureRaw() - 40));
            this.minTemperatureCelsius = _tmp;
            return this.minTemperatureCelsius;
        }

        private int maxTemperatureRaw;
        private int maxCellNo;
        private int minTemperatureRaw;
        private int minCellNo;
        private Daly _root;
        private Daly.Frame _parent;

        public int maxTemperatureRaw() {
            return maxTemperatureRaw;
        }

        public int maxCellNo() {
            return maxCellNo;
        }

        public int minTemperatureRaw() {
            return minTemperatureRaw;
        }

        public int minCellNo() {
            return minCellNo;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class CellVoltage extends KaitaiStruct {
        public static CellVoltage fromFile(String fileName) throws IOException {
            return new CellVoltage(new ByteBufferKaitaiStream(fileName));
        }

        public CellVoltage(KaitaiStream _io) {
            this(_io, null, null);
        }

        public CellVoltage(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public CellVoltage(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.frameNumber = this._io.readU1();
            voltageMv = new ArrayList<Integer>(((Number) (3)).intValue());
            for (int i = 0; i < 3; i++) {
                this.voltageMv.add(this._io.readU2be());
            }
        }

        private int frameNumber;
        private ArrayList<Integer> voltageMv;
        private Daly _root;
        private Daly.Frame _parent;

        public int frameNumber() {
            return frameNumber;
        }

        public ArrayList<Integer> voltageMv() {
            return voltageMv;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class SocVoltageCurrent extends KaitaiStruct {
        public static SocVoltageCurrent fromFile(String fileName) throws IOException {
            return new SocVoltageCurrent(new ByteBufferKaitaiStream(fileName));
        }

        public SocVoltageCurrent(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SocVoltageCurrent(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public SocVoltageCurrent(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.pressureRaw = this._io.readU2be();
            this.acquisitionRaw = this._io.readU2be();
            this.currentRaw = this._io.readU2be();
            this.socRaw = this._io.readU2be();
        }

        private Double pressureV;

        public Double pressureV() {
            if (this.pressureV != null) {
                return this.pressureV;
            }
            double _tmp = ((pressureRaw() / 10.0));
            this.pressureV = _tmp;
            return this.pressureV;
        }

        private Double acquisitionV;

        public Double acquisitionV() {
            if (this.acquisitionV != null) {
                return this.acquisitionV;
            }
            double _tmp = ((acquisitionRaw() / 10.0));
            this.acquisitionV = _tmp;
            return this.acquisitionV;
        }

        private Double currentA;

        public Double currentA() {
            if (this.currentA != null) {
                return this.currentA;
            }
            double _tmp = (((currentRaw() - 30000) / 10.0));
            this.currentA = _tmp;
            return this.currentA;
        }

        private Double socPercent;

        public Double socPercent() {
            if (this.socPercent != null) {
                return this.socPercent;
            }
            double _tmp = ((socRaw() / 10.0));
            this.socPercent = _tmp;
            return this.socPercent;
        }

        private int pressureRaw;
        private int acquisitionRaw;
        private int currentRaw;
        private int socRaw;
        private Daly _root;
        private Daly.Frame _parent;

        public int pressureRaw() {
            return pressureRaw;
        }

        public int acquisitionRaw() {
            return acquisitionRaw;
        }

        public int currentRaw() {
            return currentRaw;
        }

        public int socRaw() {
            return socRaw;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class Frame extends KaitaiStruct {
        public static Frame fromFile(String fileName) throws IOException {
            return new Frame(new ByteBufferKaitaiStream(fileName));
        }

        public Frame(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Frame(KaitaiStream _io, Daly _parent) {
            this(_io, _parent, null);
        }

        public Frame(KaitaiStream _io, Daly _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.magic = this._io.readBytes(2);
            if (!(Arrays.equals(magic(), new byte[] { -91, 1 }))) {
                throw new KaitaiStream.ValidationNotEqualError(new byte[] { -91, 1 }, magic(), _io(),
                        "/types/frame/seq/0");
            }
            this.dataId = this._io.readU1();
            this.length = this._io.readU1();
            switch (dataId()) {
                case 146: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new MinMaxTemperature(_io__raw_data, this, _root);
                    break;
                }
                case 150: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new MonomerTemperature(_io__raw_data, this, _root);
                    break;
                }
                case 145: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new MinMaxVoltage(_io__raw_data, this, _root);
                    break;
                }
                case 144: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new SocVoltageCurrent(_io__raw_data, this, _root);
                    break;
                }
                case 149: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new CellVoltage(_io__raw_data, this, _root);
                    break;
                }
                case 148: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new StatusInformation(_io__raw_data, this, _root);
                    break;
                }
                case 152: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new BatteryFailureStatus(_io__raw_data, this, _root);
                    break;
                }
                case 151: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new MonomerEquilibriumState(_io__raw_data, this, _root);
                    break;
                }
                case 147: {
                    this._raw_data = this._io.readBytes(length());
                    KaitaiStream _io__raw_data = new ByteBufferKaitaiStream(_raw_data);
                    this.data = new ChargeDischargeStatus(_io__raw_data, this, _root);
                    break;
                }
                default: {
                    this.data = this._io.readBytes(length());
                    break;
                }
            }
            this.checksum = this._io.readU1();
        }

        private byte[] magic;
        private int dataId;
        private int length;
        private Object data;
        private int checksum;
        private Daly _root;
        private Daly _parent;
        private byte[] _raw_data;

        public byte[] magic() {
            return magic;
        }

        public int dataId() {
            return dataId;
        }

        public int length() {
            return length;
        }

        public Object data() {
            return data;
        }

        public int checksum() {
            return checksum;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly _parent() {
            return _parent;
        }

        public byte[] _raw_data() {
            return _raw_data;
        }
    }

    public static class StatusInformation extends KaitaiStruct {
        public static StatusInformation fromFile(String fileName) throws IOException {
            return new StatusInformation(new ByteBufferKaitaiStream(fileName));
        }

        public enum ChargerStatus {
            DISCONNECTED(0),
            CONNECTED(1);

            private final long id;

            ChargerStatus(long id) {
                this.id = id;
            }

            public long id() {
                return id;
            }

            private static final Map<Long, ChargerStatus> byId = new HashMap<Long, ChargerStatus>(2);
            static {
                for (ChargerStatus e : ChargerStatus.values()) {
                    byId.put(e.id(), e);
                }
            }

            public static ChargerStatus byId(long id) {
                return byId.get(id);
            }
        }

        public enum LoadStatus {
            DISCONNECTED(0),
            ACCESS(1);

            private final long id;

            LoadStatus(long id) {
                this.id = id;
            }

            public long id() {
                return id;
            }

            private static final Map<Long, LoadStatus> byId = new HashMap<Long, LoadStatus>(2);
            static {
                for (LoadStatus e : LoadStatus.values()) {
                    byId.put(e.id(), e);
                }
            }

            public static LoadStatus byId(long id) {
                return byId.get(id);
            }
        }

        public StatusInformation(KaitaiStream _io) {
            this(_io, null, null);
        }

        public StatusInformation(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public StatusInformation(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.batteryString = this._io.readU1();
            this.temperature = this._io.readU1();
            this.chargerStatus = ChargerStatus.byId(this._io.readU1());
            this.loadStatus = LoadStatus.byId(this._io.readU1());
            this.di1 = this._io.readBitsIntBe(1) != 0;
            this.di2 = this._io.readBitsIntBe(1) != 0;
            this.di3 = this._io.readBitsIntBe(1) != 0;
            this.di4 = this._io.readBitsIntBe(1) != 0;
            this.do1 = this._io.readBitsIntBe(1) != 0;
            this.do2 = this._io.readBitsIntBe(1) != 0;
            this.do3 = this._io.readBitsIntBe(1) != 0;
            this.do4 = this._io.readBitsIntBe(1) != 0;
            this._io.alignToByte();
            this.chargeDischargeCycles = this._io.readU2be();
        }

        private int batteryString;
        private int temperature;
        private ChargerStatus chargerStatus;
        private LoadStatus loadStatus;
        private boolean di1;
        private boolean di2;
        private boolean di3;
        private boolean di4;
        private boolean do1;
        private boolean do2;
        private boolean do3;
        private boolean do4;
        private int chargeDischargeCycles;
        private Daly _root;
        private Daly.Frame _parent;

        public int batteryString() {
            return batteryString;
        }

        public int temperature() {
            return temperature;
        }

        public ChargerStatus chargerStatus() {
            return chargerStatus;
        }

        public LoadStatus loadStatus() {
            return loadStatus;
        }

        public boolean di1() {
            return di1;
        }

        public boolean di2() {
            return di2;
        }

        public boolean di3() {
            return di3;
        }

        public boolean di4() {
            return di4;
        }

        public boolean do1() {
            return do1;
        }

        public boolean do2() {
            return do2;
        }

        public boolean do3() {
            return do3;
        }

        public boolean do4() {
            return do4;
        }

        public int chargeDischargeCycles() {
            return chargeDischargeCycles;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class MonomerTemperature extends KaitaiStruct {
        public static MonomerTemperature fromFile(String fileName) throws IOException {
            return new MonomerTemperature(new ByteBufferKaitaiStream(fileName));
        }

        public MonomerTemperature(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MonomerTemperature(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public MonomerTemperature(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.frameNumber = this._io.readU1();
            temperature = new ArrayList<Integer>(((Number) (6)).intValue());
            for (int i = 0; i < 6; i++) {
                this.temperature.add(this._io.readU1());
            }
        }

        private int frameNumber;
        private ArrayList<Integer> temperature;
        private Daly _root;
        private Daly.Frame _parent;

        public int frameNumber() {
            return frameNumber;
        }

        public ArrayList<Integer> temperature() {
            return temperature;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class MinMaxVoltage extends KaitaiStruct {
        public static MinMaxVoltage fromFile(String fileName) throws IOException {
            return new MinMaxVoltage(new ByteBufferKaitaiStream(fileName));
        }

        public MinMaxVoltage(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MinMaxVoltage(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public MinMaxVoltage(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.maxVoltageRaw = this._io.readU2be();
            this.maxCellNo = this._io.readU1();
            this.minVoltageRaw = this._io.readU2be();
            this.minCellNo = this._io.readU1();
        }

        private Double maxVoltageV;

        public Double maxVoltageV() {
            if (this.maxVoltageV != null) {
                return this.maxVoltageV;
            }
            double _tmp = ((maxVoltageRaw() / 1000.0));
            this.maxVoltageV = _tmp;
            return this.maxVoltageV;
        }

        private Double minVoltageV;

        public Double minVoltageV() {
            if (this.minVoltageV != null) {
                return this.minVoltageV;
            }
            double _tmp = ((minVoltageRaw() / 1000.0));
            this.minVoltageV = _tmp;
            return this.minVoltageV;
        }

        private int maxVoltageRaw;
        private int maxCellNo;
        private int minVoltageRaw;
        private int minCellNo;
        private Daly _root;
        private Daly.Frame _parent;

        public int maxVoltageRaw() {
            return maxVoltageRaw;
        }

        public int maxCellNo() {
            return maxCellNo;
        }

        public int minVoltageRaw() {
            return minVoltageRaw;
        }

        public int minCellNo() {
            return minCellNo;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class MonomerEquilibriumState extends KaitaiStruct {
        public static MonomerEquilibriumState fromFile(String fileName) throws IOException {
            return new MonomerEquilibriumState(new ByteBufferKaitaiStream(fileName));
        }

        public MonomerEquilibriumState(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MonomerEquilibriumState(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public MonomerEquilibriumState(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            state = new ArrayList<Boolean>(((Number) (64)).intValue());
            for (int i = 0; i < 64; i++) {
                this.state.add(this._io.readBitsIntBe(1) != 0);
            }
        }

        private ArrayList<Boolean> state;
        private Daly _root;
        private Daly.Frame _parent;

        public ArrayList<Boolean> state() {
            return state;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class ChargeDischargeStatus extends KaitaiStruct {
        public static ChargeDischargeStatus fromFile(String fileName) throws IOException {
            return new ChargeDischargeStatus(new ByteBufferKaitaiStream(fileName));
        }

        public enum ChargeDischarge {
            STATIONARY(0),
            CHARGING(1),
            DISCHARGING(2);

            private final long id;

            ChargeDischarge(long id) {
                this.id = id;
            }

            public long id() {
                return id;
            }

            private static final Map<Long, ChargeDischarge> byId = new HashMap<Long, ChargeDischarge>(3);
            static {
                for (ChargeDischarge e : ChargeDischarge.values()) {
                    byId.put(e.id(), e);
                }
            }

            public static ChargeDischarge byId(long id) {
                return byId.get(id);
            }
        }

        public ChargeDischargeStatus(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ChargeDischargeStatus(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public ChargeDischargeStatus(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            this.chargeDischargeStatusRaw = ChargeDischarge.byId(this._io.readU1());
            this.chargingMosTubeStatus = this._io.readU1();
            this.dischargeMosTubeState = this._io.readU1();
            this.bmsLife = this._io.readU1();
            this.residualCapacityRaw = this._io.readU4be();
        }

        private Double residualCapacityAh;

        public Double residualCapacityAh() {
            if (this.residualCapacityAh != null) {
                return this.residualCapacityAh;
            }
            double _tmp = ((residualCapacityRaw() / 1000.0));
            this.residualCapacityAh = _tmp;
            return this.residualCapacityAh;
        }

        private ChargeDischarge chargeDischargeStatusRaw;
        private int chargingMosTubeStatus;
        private int dischargeMosTubeState;
        private int bmsLife;
        private long residualCapacityRaw;
        private Daly _root;
        private Daly.Frame _parent;

        public ChargeDischarge chargeDischargeStatusRaw() {
            return chargeDischargeStatusRaw;
        }

        public int chargingMosTubeStatus() {
            return chargingMosTubeStatus;
        }

        public int dischargeMosTubeState() {
            return dischargeMosTubeState;
        }

        public int bmsLife() {
            return bmsLife;
        }

        public long residualCapacityRaw() {
            return residualCapacityRaw;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    public static class BatteryFailureStatus extends KaitaiStruct {
        public static BatteryFailureStatus fromFile(String fileName) throws IOException {
            return new BatteryFailureStatus(new ByteBufferKaitaiStream(fileName));
        }

        public BatteryFailureStatus(KaitaiStream _io) {
            this(_io, null, null);
        }

        public BatteryFailureStatus(KaitaiStream _io, Daly.Frame _parent) {
            this(_io, _parent, null);
        }

        public BatteryFailureStatus(KaitaiStream _io, Daly.Frame _parent, Daly _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }

        private void _read() {
            error = new ArrayList<Integer>(((Number) (7)).intValue());
            for (int i = 0; i < 7; i++) {
                this.error.add(this._io.readU1());
            }
            this.faultCode = this._io.readU1();
        }

        private ArrayList<Integer> error;
        private int faultCode;
        private Daly _root;
        private Daly.Frame _parent;

        public ArrayList<Integer> error() {
            return error;
        }

        public int faultCode() {
            return faultCode;
        }

        public Daly _root() {
            return _root;
        }

        @Override
        public Daly.Frame _parent() {
            return _parent;
        }
    }

    private ArrayList<Frame> frame;
    private Daly _root;
    private KaitaiStruct _parent;

    public ArrayList<Frame> frame() {
        return frame;
    }

    public Daly _root() {
        return _root;
    }

    @Override
    public KaitaiStruct _parent() {
        return _parent;
    }
}
