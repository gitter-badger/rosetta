/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucar.unidata.rosetta;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;

import java.io.File;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Rosetta {

    /** Convert ASCII CSV file (simple, one station, one time per row) into netCDF
     * using metadata defined in the ncml file */


    /**
     * Convert the list of list data, as obtained from the ascii file, into a netCDF file
     * using the metadata defined in the ncml template
     *
     * @param ncmlFile path to ncml template
     * @param fileOut path of netCDF output file
     * @param outerList data list-of-lists
     *
     * @return true, if successful, false if not
     */
    public boolean convert(String ncmlFile, String fileOut,
                           List<List<String>> outerList) {

        try {
            NetcdfDataset ncd = NcMLReader.readNcML("file://" + ncmlFile,
                                    null);
            FileWriter2 ncdnew = new ucar.nc2.FileWriter2(ncd, fileOut, NetcdfFileWriter.Version.netcdf3, null);
            NetcdfFile ncout = ncdnew.write();
            ncd.close();
            ncout.close();

            NetcdfFileWriter ncFileWriter =
               NetcdfFileWriter.openExisting(fileOut);

            NetcdfFile ncfile = ncFileWriter.getNetcdfFile();
            List<Variable> ncFileVariables = ncfile.getVariables();
            // get time dim
            Dimension timeDim       = ncfile.findDimension("time");
            Iterator  ncVarIterator = ncFileVariables.iterator();
            while (ncVarIterator.hasNext()) {
                Variable  theVar  = (Variable) ncVarIterator.next();
                String    varName = theVar.getFullName();
                Attribute attr    = theVar.findAttribute("_columnId");
                DataType  dt      = theVar.getDataType();
                if (attr != null) {
                    int varIndex = Integer.parseInt(attr.getStringValue());
                    int len      = outerList.size();
                    if (dt.equals(DataType.FLOAT)) {
                        ArrayFloat.D1 vals =
                            new ArrayFloat.D1(outerList.size());
                        int      i                 = 0;
                        for (List<String> innerList : outerList) {
                            float f = Float.parseFloat(
                                          innerList.get(
                                              varIndex));
                            vals.set(i, f);
                            i++;
                        }
                        ncFileWriter.write(theVar, vals);
                    } else if (dt.equals(DataType.INT)) {
                        ArrayInt.D1 vals =
                                new ArrayInt.D1(outerList.size());
                        int      i                 = 0;
                        for (List<String> innerList : outerList) {
                            int f = Integer.parseInt(
                                    innerList.get(
                                            varIndex));
                            vals.set(i, f);
                            i++;
                        }
                        ncFileWriter.write(theVar, vals);
                    } else if (dt.equals(DataType.CHAR)) {
                        assert theVar.getRank() == 2;
                        int elementLength = theVar.getDimension(1).getLength();

                        ArrayChar.D2 vals =
                            new ArrayChar.D2(outerList.size(), elementLength);
                        int      i                 = 0;
                        for (List<String> innerList : outerList) {

                            String f = innerList.get(varIndex);
                            vals.setString(i,f);
                            i++;
                        }
                        ncFileWriter.write(theVar, vals);
                    } else {
                    }
                }
            }
            ncfile.close();

            File file = new File(fileOut);

            if (file.exists()) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            //log.error("IOException: " + e.getMessage());
            return false;
        } catch (InvalidRangeException e) {
            //log.error("InvalidRangeException: " + e.getMessage());
            return false;
        }

    }


    public static void main(String[] args) {
        String ncmlFile =
            "/Users/lesserwhirls/dev/unidata/rosetta/rosetta/src/edu/ucar/unidata/rosetta/test/test.ncml";
        String fileOutName =
            "/Users/lesserwhirls/dev/unidata/rosetta/rosetta/src/edu/ucar/unidata/rosetta/test/rosetta_test.nc";
        Rosetta pz        = new Rosetta();
        ArrayList<List<String>> outerList = new ArrayList<List<String>>();
        for (int j = 0; j < 10; j++) {
            ArrayList<String> innerList = new ArrayList<String>();
            for (int i = 0; i < 11; i++) {
                innerList.add(Integer.toString((i + j) * i));
            }
            outerList.add(innerList);
        }
        pz.convert(ncmlFile, fileOutName, outerList);
    }
}