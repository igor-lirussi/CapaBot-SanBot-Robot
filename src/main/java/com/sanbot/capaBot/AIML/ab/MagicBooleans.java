package com.sanbot.capaBot.AIML.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/

/**
 * Global boolean values that control various actions in Program AB
 */
public class MagicBooleans {
    public static boolean trace_mode = true;
    public static boolean enable_external_sets = true;
    public static boolean enable_external_maps = true;
    public static boolean jp_tokenize = false;
    public static boolean fix_excel_csv = true;
    public static boolean enable_network_connection = true;
    public static boolean cache_sraix = false;
    public static boolean qa_test_mode = false;
    public static boolean make_verbs_sets_maps = false;

	public static void trace(String traceString) {
 		if (trace_mode) {
 			System.out.println(traceString);
 		}
 	}
}
