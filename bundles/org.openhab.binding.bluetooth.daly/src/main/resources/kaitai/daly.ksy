meta:
  id: daly
  endian: be
seq:
  - id: frame
    type: frame
    repeat: eos
types:
  frame:
    seq:
    - id: magic
      contents: [0xa5, 0x01]
    - id: data_id
      type: u1
    - id: length
      type: u1
    - id: data
      size: length
      type:
        switch-on: data_id
        cases:
          0x90: soc_voltage_current
          0x91: min_max_voltage
          0x92: min_max_temperature
          0x93: charge_discharge_status
          0x94: status_information
          0x95: cell_voltage
          0x96: monomer_temperature
          0x97: monomer_equilibrium_state
          0x98: battery_failure_status
    - id: checksum
      type: u1
  soc_voltage_current:
    seq:
      - id: pressure_raw
        type: u2
      - id: acquisition_raw
        type: u2
      - id: current_raw
        type: u2
      - id: soc_raw
        type: u2
    instances:
      pressure_v:
        value: pressure_raw / 10.0
      acquisition_v:
        value: acquisition_raw / 10.0
      current_a:
        value: (current_raw - 30000) / 10.0
      soc_percent:
        value: soc_raw / 10.0
  min_max_voltage:
    seq:
      - id: max_voltage_raw
        type: u2
      - id: max_cell_no
        type: u1
      - id: min_voltage_raw
        type: u2
      - id: min_cell_no
        type: u1
    instances:
      max_voltage_v:
        value: max_voltage_raw / 1000.0
      min_voltage_v:
        value: min_voltage_raw / 1000.0
  min_max_temperature:
    seq:
      - id: max_temperature_raw
        type: u1
      - id: max_cell_no
        type: u1
      - id: min_temperature_raw
        type: u1
      - id: min_cell_no
        type: u1
    instances:
      max_temperature_celsius:
        value: max_temperature_raw - 40
      min_temperature_celsius:
        value: min_temperature_raw - 40
  charge_discharge_status:
    seq:
      - id: charge_discharge_status_raw
        type: u1
        enum: charge_discharge
      - id: charging_mos_tube_status
        type: u1
      - id: discharge_mos_tube_state
        type: u1
      - id: bms_life
        type: u1
      - id: residual_capacity_raw
        type: u4
    enums:
      charge_discharge:
        0: stationary
        1: charging
        2: discharging
    instances:
      residual_capacity_ah:
        value: residual_capacity_raw / 1000.0
  status_information:
    seq:
      - id: battery_string
        type: u1
      - id: temperature
        type: u1
      - id: charger_status
        type: u1
        enum: charger_status
      - id: load_status
        type: u1
        enum: load_status
      - id: di_1
        type: b1
      - id: di_2
        type: b1
      - id: di_3
        type: b1
      - id: di_4
        type: b1
      - id: do_1
        type: b1
      - id: do_2
        type: b1
      - id: do_3
        type: b1
      - id: do_4
        type: b1
      - id: charge_discharge_cycles
        type: u2
    enums:
      charger_status:
        0: disconnected
        1: connected
      load_status:
        0: disconnected
        1: access
  cell_voltage:
    seq:
      - id: frame_number
        type: u1
      - id: voltage_mv
        type: u2
        repeat: expr
        repeat-expr: 3
  monomer_temperature:
    seq:
      - id: frame_number
        type: u1
      - id: temperature
        type: u1
        repeat: expr
        repeat-expr: 6
  monomer_equilibrium_state:
    seq:
      - id: state
        type: b1
        repeat: expr
        repeat-expr: 64
  battery_failure_status:
    seq:
      - id: error
        type: u1
        repeat: expr
        repeat-expr: 7
      - id: fault_code
        type: u1