/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 */

/*
 *    ConversionFailure.java
 *    Copyright (C) 2018 Philip Heller
 *    
 */



package coarbitrator;


public enum ConversionFailure 
{
	PROTEIN_GP_PAGE_NO_INITIAL_RESPONSE,
	PROTEIN_GP_PAGE_NO_ID_TAG_IN_INITIAL_RESPONSE,
	PROTEIN_GP_PAGE_NO_GP_PAGE,
	PROTEIN_GP_PAGE_NO_CODED_BY_TAG,
	PROTEIN_GP_PAGE_BAD_NUMBER_FORMAT,
	NUCLEOTIDE_PAGE_NOT_RECEIVED,
	NUCLEOTIDE_PAGE_NOT_CONVERTED_TO_EMBL,
	EMBL_FILE_MISSING
}
