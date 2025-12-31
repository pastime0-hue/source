package com.dsic.dsicfota.Utils;

public class SerialNumberParser {
    private enum YEAR_CODE{
        Y_UNKNOWN('!'),
        Y_2014('G'),
        Y_2015('H'),
        Y_2016('I'),
        Y_2017('J'),
        Y_2018('K'),
        Y_2019('L'),
        Y_2020('M'),
        Y_2021('O'),
        Y_2022('P'),
        Y_2023('Q'),
        Y_2024('R'),
        Y_2025('S'),
        Y_2026('T'),
        Y_2027('U'),
        Y_2028('V'),
        Y_2029('W'),
        Y_2030('X'),
        Y_2031('Y'),
        Y_2032('Z');

        private char _value = ' ';
        YEAR_CODE(char value){
            _value = value;
        }

        public boolean fromChar(char value){
            for (YEAR_CODE year_code : YEAR_CODE.values()){
                if(year_code.getValue() == value){
                    _value = value;
                    return true;
                }
            }
            return false;
        }

        public int getYearNumber(){
            int value = 0;
            switch (_value){
                case 'G': value = 2014; break;
                case 'H': value = 2015; break;
                case 'I': value = 2016; break;
                case 'J': value = 2017; break;
                case 'K': value = 2018; break;
                case 'L': value = 2019; break;
                case 'M': value = 2020; break;
                case 'O': value = 2021; break;
                case 'P': value = 2022; break;
                case 'Q': value = 2023; break;
                case 'R': value = 2024; break;
                case 'S': value = 2025; break;
                case 'T': value = 2026; break;
                case 'U': value = 2027; break;
                case 'V': value = 2028; break;
                case 'W': value = 2029; break;
                case 'X': value = 2030; break;
                case 'Y': value = 2031; break;
                case 'Z': value = 2032; break;
            }
            return value;
        }

        public char getValue(){
            return _value;
        }


    }

    private enum MONTH_CODE{
        M_UNKNOWN('!'),
        M_1('A'),
        M_2('B'),
        M_3('C'),
        M_4('D'),
        M_5('E'),
        M_6('F'),
        M_7('G'),
        M_8('H'),
        M_9('I'),
        M_10('J'),
        M_11('K'),
        M_12('L');


        private char _value = ' ';
        MONTH_CODE(char value){
            _value = value;
        }

        public int getMonthNumber(){
            int value = 0;
            switch (_value){
                case 'A': value = 1; break;
                case 'B': value = 2; break;
                case 'C': value = 3; break;
                case 'D': value = 4; break;
                case 'E': value = 5; break;
                case 'F': value = 6; break;
                case 'G': value = 7; break;
                case 'H': value = 8; break;
                case 'I': value = 9; break;
                case 'J': value = 10; break;
                case 'K': value = 11; break;
                case 'L': value = 12; break;
            }
            return value;
        }

        public boolean fromChar(char value){
            for (MONTH_CODE month_code : MONTH_CODE.values()){
                if(month_code.getValue() == value){
                    _value = value;
                    return true;
                }
            }
            return false;
        }

        public char getValue(){
            return _value;
        }
    }

    private YEAR_CODE _year_code = YEAR_CODE.Y_UNKNOWN;
    private MONTH_CODE _month_code = MONTH_CODE.M_UNKNOWN;

    public SerialNumberParser(){

    }

    public boolean parse_serial_number(String serial_number){
        if(serial_number == null){
            //LOG.ERR("parse_serial_number->serial number is null");
            return false;
        }
        if( serial_number.length() < 9){
            //LOG.ERR("parse_serial_number->serial number is too short:"+serial_number);
            return false;
        }

        if(_month_code.fromChar(serial_number.charAt(8)) == false){
            //LOG.ERR("parse_serial_number->get month fail:"+serial_number);
            return false;
        }

        if(_year_code.fromChar(serial_number.charAt(7)) == false){
            //LOG.ERR("parse_serial_number->get year fail:"+serial_number);
            return false;
        }
        return true;
    }

    public String get_date(){
        final int YEAR = _year_code.getYearNumber();
        final int MONTH = _month_code.getMonthNumber();
        if(YEAR == 0 || MONTH == 0){
            LOG.ERR("get data fail->year:"+YEAR+":month:"+MONTH);
            return null;
        }
        String date = String.valueOf(YEAR)+String.format("%02d",MONTH);
        return date;
    }

}
