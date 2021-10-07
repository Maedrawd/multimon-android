cmake_minimum_required(VERSION 3.10.2)

project("multimon")

add_library( # Sets the name of the library.
        multimon

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        multimon.h
        multimon.c

        #gen.h
        filter.h
        filter-i386.h

        #unixinput.c
        uart.c
        pocsag.c
        selcall.c
        hdlc.c
        demod_zvei1.c
        demod_zvei2.c
        demod_zvei3.c
        demod_pzvei.c
        demod_dzvei.c
        demod_ccir.c
        demod_eia.c
        demod_eea.c
        demod_ufsk12.c
        demod_poc24.c
        demod_poc12.c
        demod_poc5.c
        demod_hapn48.c
        demod_fsk96.c
        demod_dtmf.c
        demod_clipfsk.c
        demod_fmsfsk.c
        demod_afsk24.c
        demod_afsk24_3.c
        demod_afsk24_2.c
        demod_afsk12.c
        #demod_flex.c
        costabi.c
        costabf.c
        clip.c
        fms.c
        demod_eas.c
        demod_morse.c
        demod_dumpcsv.c
        demod_x10.c)

find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        log-lib

        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log )

target_link_libraries(multimon
        android
        log)