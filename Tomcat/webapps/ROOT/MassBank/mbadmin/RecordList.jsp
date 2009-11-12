<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%
/*******************************************************************************
 *
 * Copyright (C) 2009 JST-BIRD MassBank
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *******************************************************************************
 *
 * レコード一覧
 *
 * ver 1.0.2 2009.09.04
 *
 ******************************************************************************/
%>

<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.PrintStream" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLConnection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="massbank.FileUpload" %>
<%@ page import="massbank.GetConfig" %>
<%@ page import="massbank.Sanitizer" %>
<%@ page import="massbank.admin.AdminCommon" %>
<%@ page import="massbank.admin.DatabaseAccess" %>
<%@ page import="massbank.admin.FileUtil" %>
<%@ page import="massbank.admin.OperationManager" %>
<%!
	/** 作業ディレクトリ用日時フォーマット */
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss_SSS");
	
	/** 改行文字列 */
	private final String NEW_LINE = System.getProperty("line.separator");
	
	/** ステータス（OK） */
	private final String STATUS_OK = "<span class=\"msgFont\">ok</span>";
	
	/** ステータス（警告） */
	private final String STATUS_WARN = "<span class=\"warnFont\">warn</span>";
	
	/** ステータス（エラー） */
	private final String STATUS_ERR = "<span class=\"errFont\">error</span>";
	
	/** テーブル情報 */
	private final String[][] tableInfo = {
	    {"SPECTRUM", "true"},
	    {"RECORD", "true"},
	    {"PEAK", "false"},
	    {"CH_NAME", "false"},
	    {"CH_LINK", "fasle"},
	    {"TREE", "false"}
	};
	
	/**
	 * HTML表示用メッセージテンプレート（情報）
	 * @param msg メッセージ（情報）
	 * @return 表示用メッセージ（情報）
	 */
	private String msgInfo(String msg) {
		StringBuilder sb = new StringBuilder( "<i>info</i> : <span class=\"msgFont\">" );
		sb.append( msg );
		sb.append( "</span><br>" );
		return sb.toString();
	}
	
	/**
	 * HTML表示用メッセージテンプレート（警告）
	 * @param msg メッセージ（警告）
	 * @return 表示用メッセージ（警告）
	 */
	private String msgWarn(String msg) {
		StringBuilder sb = new StringBuilder( "<i>warn</i> : <span class=\"warnFont\">" );
		sb.append( msg );
		sb.append( "</span><br>" );
		return sb.toString();
	}
	
	/**
	 * HTML表示用メッセージテンプレート（エラー）
	 * @param msg メッセージ（エラー）
	 * @return 表示用メッセージ（エラー）
	 */
	private String msgErr(String msg) {
		StringBuilder sb = new StringBuilder( "<i>error</i> : <span class=\"errFont\">" );
		sb.append( msg );
		sb.append( "</span><br>" );
		return sb.toString();
	}
	
	/**
	 * レコード一覧表示処理
	 * @param db DBアクセスオブジェクト
	 * @param op JspWriter出力バッファ
	 * @param reqUrl リクエストURL
	 * @param selDbName DB名
	 * @param recPath レコードファイル格納パス
	 * @param recUrl レコード格納URL
	 * @return 結果
	 * @throws IOException
	 */ 
	private boolean dispRecord( DatabaseAccess db, JspWriter op, String reqUrl, String selDbName, String recPath, String recUrl ) throws IOException {
		
		ArrayList<String> regInst = new ArrayList<String>();				// DBに存在する装置ID
		TreeMap<String, String> mainList = new TreeMap<String, String>();	// DBに存在するIDリスト
		TreeMap<String, String> subList = new TreeMap<String, String>();	// ファイルのみ存在するリスト
		TreeMap<String, String> instList = new TreeMap<String, String>();	// DBに存在するIDリストに対応づけされている装置IDのリスト
		
		//----------------------------------------------------
		// DBから登録情報取得
		//----------------------------------------------------
		String sql = "SELECT INSTRUMENT_NO FROM INSTRUMENT;";
		String[] sqls = {
		    "SELECT ID, NAME FROM SPECTRUM ORDER BY ID;",
		    "SELECT ID, INSTRUMENT_NO FROM RECORD ORDER BY ID;",
		    "SELECT ID FROM PEAK GROUP BY ID ORDER BY ID;",
		    "SELECT ID FROM CH_NAME ID ORDER BY ID;",
		    "SELECT ID FROM CH_LINK ID ORDER BY ID;",
		    "SELECT ID FROM TREE WHERE ID IS NOT NULL AND ID<>'' ORDER BY ID;"
		};
		ArrayList<String>[] sqlResults = (ArrayList<String>[])new ArrayList[sqls.length];
		ResultSet rs = null;
		ResultSet[] rss = new ResultSet[sqls.length];
		String execSql = "";
		try {
			// INSTRUMENTテーブルから登録されている装置IDを取得
			execSql = sql;
			rs = db.executeQuery(execSql);
			while ( rs.next() ) {
				String regInstNo = rs.getString("INSTRUMENT_NO");
				regInst.add(regInstNo);
			}
			
			// 各テーブルから存在するIDを取得
			for ( int i=0; i<sqls.length; i++ ) {
				sqlResults[i] = new ArrayList<String>();
				execSql = sqls[i];
				rss[i] = db.executeQuery(execSql);
				while ( rss[i].next() ) {
					String id = rss[i].getString("ID");
					sqlResults[i].add(id);
					if ( !tableInfo[i][0].equals("SPECTRUM") ) {
						if ( !mainList.containsKey(id) ) {
							mainList.put(id, "");
						}
					}
					else {
						mainList.put(id, rss[i].getString("NAME"));
					}
					if ( tableInfo[i][0].equals("RECORD") ) {
						String instNo = rss[i].getString("INSTRUMENT_NO");
						instList.put(id, instNo);
					}
				}
			}
		}
		catch (SQLException e) {
			Logger.global.severe( "SQL : " + execSql );
			e.printStackTrace();
			op.println( msgErr( "database access error." ) );
			return false;
		}
		finally {
			try {
				if ( rs != null ) { rs.close(); }
				for (int i=0; i<rss.length; i++) {
					if ( rss[i] != null ) { rss[i].close(); }
				}
			}
			catch (SQLException e) {}
		}
		
		// レコードファイルパス内の全ファイルおよびフォルダ名退避
		ArrayList<String> recFiles = new ArrayList(Arrays.asList((new File( recPath )).list()));
		recFiles.remove("Copyright");
		
		// 表示情報0件
		if ( mainList.size() == 0 && recFiles.size() == 0 ) {
			op.println( msgInfo( "no record." ) );
			return true;
		}
		
		//----------------------------------------------------
		// 一覧表示情報生成
		//----------------------------------------------------
		int regCnt = 0;
		StringBuilder val;
		StringBuilder details;
		for ( Map.Entry<String, String> e : mainList.entrySet() ) {
			String id = e.getKey();
			String name = e.getValue();
			boolean isError = false;
			val = new StringBuilder();
			details = new StringBuilder();
			
			val.append( name );
			val.append( "\t" );
			if ( !recFiles.remove(id + ".txt") ) {
				val.append( "\t" );
				details.append( "<span class=\"errFont\">file not exist.</span><br />" );
			}
			else {
				val.append( recUrl + id + "\t" );
			}
			// INSTRUMENTテーブル登録済みチェック
			if ( instList.containsKey(id) ) {
				if ( !regInst.contains(instList.get(id)) ) {
					details.append( "<span class=\"errFont\">unregistered. [<i>INSTRUMENT</i>]</span><br />" );
				}
			}
			else {
				details.append( "<span class=\"errFont\">unregistered. [<i>INSTRUMENT</i>]</span><br />" );
			}
			// 各テーブル登録済みチェック
			for ( int i=0; i<sqlResults.length; i++ ) {
				if ( !sqlResults[i].contains(id) ) {
					// CH_LINK は必須項目でないためエラーも警告も表示しない
					if ( tableInfo[i][0].equals("CH_LINK") ) {
						continue;
					}
					if ( Boolean.valueOf(tableInfo[i][1]) ) {
						details.append( "<span class=\"errFont\">unregistered. [<i>" + tableInfo[i][0] + "</i>]</span><br />" );
					}
					else {
						details.append( "<span class=\"warnFont\">unregistered. [<i>" + tableInfo[i][0] + "</i>]</span><br />" );
					}
				}
			}
			if (details.length() > 0) {
				details.delete(details.lastIndexOf("<br />"), details.length());
			}
			else {
				details.append( " " );
			}
			if ( details.indexOf("errFont") == -1 ) {
				regCnt++;
				if (details.indexOf("warnFont") == -1) {
					val.append( STATUS_OK + "\t" );
				}
				else {
					val.append( STATUS_WARN + "\t" );
				}
			}
			else {
				val.append( STATUS_ERR + "\t" );
			}
			
			val.append( details.toString() );
			mainList.put(id, val.toString());
		}
		
		for (String name : recFiles) {
			File tmp = new File(recPath + "/" + name);
			if ( tmp.isFile() ) {
				String recIdStr = "";
				details = new StringBuilder();
				
				if (name.lastIndexOf(".") > 0) {
					recIdStr = name.substring(0, name.lastIndexOf("."));
				}
				else {
					recIdStr = name;
				}
				details.append( "<span class=\"warnFont\">file only. [<i>" + name + "</i>]</span>\t" );
				val = new StringBuilder();
				val.append( details.toString() );
				
				subList.put(recIdStr, val.toString());
			}
		}
		
		//----------------------------------------------------
		// テーブルヘッダー部生成
		//----------------------------------------------------
		NumberFormat nf = NumberFormat.getNumberInstance();
		op.println( "<form name=\"formList\" action=\"" + reqUrl + "\" method=\"post\" onSubmit=\"doWait();\">" );
		op.println( "\t<input type=\"submit\" value=\"Delete\" onClick=\"return beforeDelete();\">" );
		op.println( "\t<div class=\"count baseFont\">" + nf.format(regCnt) + " registered / " + nf.format(mainList.size()) + " records&nbsp;</div>" );
		op.println( "\t<table table width=\"980\" cellspacing=\"1\" cellpadding=\"0\" bgcolor=\"Lavender\">" );
		op.println( "\t\t<tr class=\"rowHeader\">");
		op.println( "\t\t\t<td width=\"25\"><input type=\"checkbox\" name=\"chkAll\" onClick=\"checkAll();\"></td>" );
		op.println( "\t\t\t<td width=\"95\">ID</td>" );
		op.println( "\t\t\t<td width=\"440\">Record Title</td>" );
		op.println( "\t\t\t<td width=\"70\">Record</td>" );
		op.println( "\t\t\t<td width=\"70\">Status</td>" );
		op.println( "\t\t\t<td width=\"200\">Details</td>" );
		op.println( "\t\t</tr>");
		
		//----------------------------------------------------
		// レコード有無一覧表示
		//----------------------------------------------------
		int cnt = 0;
		String strId;
		String[] vals;
		for ( Map.Entry<String, String> e : mainList.entrySet() ) {
			strId = e.getKey();
			vals = e.getValue().split("\t");
			
			String strViewEvent = "";
			String strRecName = "";
			String strBtnDisable = "";
			if ( vals[2].indexOf("error") != -1 ) {
				strBtnDisable = " disabled";
			}
			else {
				if ( !vals[1].equals("") ) {
					strViewEvent = " onClick=\"popupRecView('" + vals[1] + "');\" ";
					strRecName = vals[1].substring(vals[1].lastIndexOf("id=") + 3);
				}
			}
			
			op.println( "\t\t<tr class=\"rowEnable\" id=\"row" + cnt + "\">" );
			op.println( "\t\t\t<td>" );
			op.println( "\t\t\t\t<input type=\"checkbox\" name=\"id\" value=\"" + strId + "\" onClick=\"check(" + cnt + ");\">" );
			op.println( "\t\t\t</td>" );
			op.println( "\t\t\t<td class=\"leftIndent\">" + strId+ "</td>" );
			op.println( "\t\t\t<td class=\"leftIndent\">" + Sanitizer.html(vals[0]) + "</td>" );
			op.println( "\t\t\t<td class=\"center\">" );
			op.println( "\t\t\t\t<input type=\"button\"" + strViewEvent + "value=\"View\" title=\"" + strRecName + "\"" + strBtnDisable + ">" );
			op.println( "\t\t\t</td>" );
			op.println( "\t\t\t<td align=\"center\">" + vals[2] + "</td>" );
			op.println( "\t\t\t<td class=\"details\">" + vals[3] + "</td>" );
			op.println( "\t\t</tr>" );
			cnt++;
		}
		
		for ( Map.Entry<String, String> e : subList.entrySet() ) {
			strId = e.getKey();
			vals = e.getValue().split("\t");
			
			op.println( "\t\t<tr class=\"rowEnable\" id=\"row" + cnt + "\">" );
			op.println( "\t\t\t<td clas=\"center\">" );
			op.println( "\t\t\t\t<input type=\"checkbox\" name=\"id\" value=\"" + strId + "\" onClick=\"check(" + cnt + ");\">" );
			op.println( "\t\t\t</td>" );
			op.println( "\t\t\t<td class=\"leftIndent\">-</td>" );
			op.println( "\t\t\t<td class=\"leftIndent\">-</td>" );
			op.println( "\t\t\t<td class=\"center\">" );
			op.println( "\t\t\t\t<input type=\"button\" value=\"View\" disabled>" );
			op.println( "\t\t\t</td>" );
			op.println( "\t\t\t<td align=\"center\"><span class=\"warnFont\">warn</span></td>" );
			op.println( "\t\t\t<td class=\"details\">" + vals[0] + "</td>" );
			op.println( "\t\t</tr>" );
			cnt++;
		}
		
		op.println( "\t</table>" );
		op.println( "\t<input type=\"hidden\" name=\"act\" value=\"\">" );
		op.println( "\t<input type=\"hidden\" name=\"db\" value=\"" + selDbName + "\">" );
		op.println( "</form>" );
		return true;
	}
	
	/**
	 * レコード情報削除処理
	 * @param db DBアクセスオブジェクト
	 * @param op JspWriter出力バッファ
	 * @param conf 設定情報
	 * @param hostName ホスト名
	 * @param recIds 削除対象のレコードID
	 * @param recPath レコード格納パス
	 * @param tmpPath 一時ディレクトリパス
	 * @param selDbName DB
	 * @return 削除レコード数（エラーの場合は-1）
	 * @throws IOException 入出力例外
	 */
	private int delRecord( DatabaseAccess db, JspWriter op, GetConfig conf, String hostName, String[] recIds, String recPath, 
	                            String tmpPath, String selDbName ) throws IOException  {
		
		if ( recIds.length == 0 ) {
			return 0;
		}
		
		//----------------------------------------------------
		// レコードファイル削除、SQLパラメータ生成
		//----------------------------------------------------
		File file = null;
		StringBuilder sqlParam = new StringBuilder();
		for ( String recId : recIds ) {
			file = new File( recPath + File.separator + recId + ".txt" );
			if ( file.isFile() ) { file.delete(); }
			sqlParam.append( "\"" + recId + "\"," );
		}
		sqlParam.setLength( sqlParam.length() - 1 );	// 最後のカンマを削除
		
		//----------------------------------------------------
		// 各テーブルからの削除処理
		//----------------------------------------------------
		String sql = "";
		try {
			sql = "START TRANSACTION;";
			db.executeUpdate( sql );
			for ( String[] info : tableInfo ) {
				if ( !info[0].equals("TREE") ) {
					sql = "DELETE FROM " + info[0] + " WHERE ID IN(" + sqlParam.toString() + ");";
					db.executeUpdate(sql);
				}
			}
			sql = "DELETE FROM TREE;";
			db.executeUpdate(sql);
			sql = "COMMIT;";
			db.executeUpdate( sql );
		}
		catch (SQLException e) {
			Logger.global.severe( "SQL : " + sql );
			e.printStackTrace();
			op.println( msgErr( "database access error." ) );
			return -1;
		}
		
		//----------------------------------------------------
		// TREEテーブル再構築処理
		//----------------------------------------------------
		// 必要な情報の取得および設定
		String[] tmpDbName = conf.getDbName();
		String[] siteLongName = conf.getSiteLongName();
		String[] urlList = conf.getSiteUrl();
		String siteName = "";
		for ( int i=0; i < tmpDbName.length; i++ ) {
			if ( tmpDbName[i].equals(selDbName) ) {
				siteName = siteLongName[i];
				break;
			}
		}
		String cgiUrl = urlList[GetConfig.MYSVR_INFO_NUM] + "cgi-bin/GenTreeSql.cgi";
		String param = "src_dir=" + recPath + "&out_dir=" + tmpPath + "&db=" + selDbName + "&name=" + siteName;
		
		// SQLファイル生成
		BufferedReader in = null;
		StringBuilder retStr = new StringBuilder();
		try {
			URL url = new URL( cgiUrl );
			URLConnection con = url.openConnection();
			con.setDoOutput(true);
			PrintStream psm = new PrintStream( con.getOutputStream() );
			psm.print( param );
			in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
			String line = "";
			String sep = System.getProperty("line.separator");
			while ( (line = in.readLine()) != null ) {
				retStr.append( line + sep );
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			op.println( msgErr( "server error." ) );
			return -1;
		}
		finally {
			try {
				if (in != null) { in.close(); }
			}
			catch (IOException e) {
			}
		}
		
		// 生成されたSQLファイルの実行
		String insertFile = tmpPath + selDbName + "_TREE.sql";
		boolean ret = FileUtil.execSqlFile(hostName, selDbName, insertFile);
		if ( !ret ) {
			Logger.global.severe( "SQLFILE : " + insertFile );
			op.println( msgErr( "sqlfile execute failed." ) );
			return -1;
		}
		
		return recIds.length;
	}
	
	/**
	 * CGI実行
	 * @param strUrl CGIのURL
	 * @param strParam CGIに渡すパラメータ
	 * @return 結果
	 */
	private boolean execCgi( String strUrl, String strParam ) {
		boolean ret = true;
		PrintStream ps = null;
		BufferedReader in = null;
		try {
			URL url = new URL( strUrl );
			URLConnection con = url.openConnection();
			con.setDoOutput(true);
			ps = new PrintStream( con.getOutputStream() );
			ps.print( strParam );
			in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
			String line = "";
			StringBuilder retStr = new StringBuilder();
			while ( (line = in.readLine()) != null ) {
				retStr.append( line + NEW_LINE );
			}
			if (retStr.indexOf("OK") == -1) {
				ret = false;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			ret = false;
		}
		finally {
			try {
				if (in != null) { in.close(); }
				if (ps != null) { ps.close(); }
			}
			catch (IOException e) {
			}
		}
		return ret;
	}
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Content-Style-Type" content="text/css">
<meta http-equiv="Content-Script-Type" content="text/javascript">
<link rel="stylesheet" type="text/css" href="css/admin.css">
<script type="text/javascript" src="../script/Common.js"></script>
<title>Admin | Record List</title>
<script language="javascript" type="text/javascript">
<!--
var OFF_COLOR = "WhiteSmoke";
var ON_COLOR = "LightSteelBlue";
var prevIndex = -1;
var ev;
window.document.onkeydown = function(e){ ev = e; }

/**
 * 削除前処理
 */
function beforeDelete() {
	
	objForm = document.formList;
	
	chkFlag = false;
	idLength = objForm.id.length;
	if ( idLength != null ) {
		// 2件以上
		for (i=0; i<idLength; i++){
			if (objForm.id[i].disabled) {
				continue;
			}
			else if (objForm.id[i].checked) {
				chkFlag = true;
				break;
			}
		}
	}
	else {
		// 1件
		if (objForm.id.checked) {
			chkFlag = true;
		}
	}
	
	if ( !chkFlag ) {
		alert("Please select one or more checkbox.");
		return false;
	}
	
	if ( confirm("are you sure?") ) {
		objForm.act.value = "del";
		return true;
	}
	else {
		return false;
	}
}

/**
 * チェックボックスチェック
 */
function check(index) {
	
	objForm = document.formList;
	idLength = objForm.id.length;
	if ( idLength == null ) {
		obj = document.getElementById( String("row0") );
		if ( objForm.id.checked ) {
			obj.style.background = ON_COLOR;
		}
		else {
			obj.style.background = OFF_COLOR;
		}
		prevIndex = -1;
		return;
	}
	
	// SHIFTキーが押されているか？
	isShift = false;
	if ( navigator.appName.indexOf("Microsoft") != -1 ) {
		// IEの場合
		if ( window.event.shiftKey ) {
			isShift = true;
		}
	}
	else {
		if ( ev != null && ev.shiftKey ) {
			isShift = true;
			ev = null;
		}
	}
	
	// SHIFTキーが押されている場合は複数行選択
	if ( isShift && prevIndex != -1 ) {
		if ( index < prevIndex ) {
			start = index; end = prevIndex;
		}
		else {
			start = prevIndex; end = index;
		}
		isCheck = objForm.id[index].checked;
		if ( isCheck ) {
			bgcolor = ON_COLOR;
		}
		else {
			bgcolor = OFF_COLOR;
		}
		for ( i = start; i <= end; i++ ) {
			objForm.id[i].checked = isCheck;
			obj = document.getElementById( String("row" + i) );
			obj.style.background = bgcolor;
		}
	}
	else {
		obj = document.getElementById( String("row" + index) );
		if ( objForm.id[index].checked ) {
			obj.style.background = ON_COLOR;
		}
		else {
			obj.style.background = OFF_COLOR;
		}
	}
	prevIndex = index;
}

/**
 * チェックボックス一括チェック
 */
function checkAll() {
	
	objForm = document.formList;
	isCheck = objForm.chkAll.checked;
	if ( isCheck ) {
		bgcolor = ON_COLOR;
	}
	else {
		bgcolor = OFF_COLOR;
	}
	
	idLength = objForm.id.length;
	if ( idLength != null ) {
		for ( i=0; i<idLength; i++ ) {
			if (objForm.id[i].disabled) {
				continue;
			}
			objForm.id[i].checked = isCheck;
			document.getElementById(String("row" + i)).style.background = bgcolor;
		}
	}
	else {
		objForm.id.checked = isCheck;
		document.getElementById(String("row0")).style.background = bgcolor;
	}
}

/**
 * DB選択
 */
function selDb() {
	url = location.href;
	url = url.split("?")[0];
	url = url.split("#")[0];
	dbVal = document.formMain.db.value;
	if ( dbVal != "" ) {
		url += "?db=" + dbVal;
	}
	location.href = url;
}

/**
 * DB選択チェック
 */
function checkDb() {
	if ( document.formMain.db.value == "" ) {
		return false;
	}
	return true;
}

/**
 * レコードファイルを新規ウインドウで開く
 * @param url ターゲットURL
 */
function popupRecView(url) {
	win = window.open(url, "RecView", "width=1024, menubar=no, resizable=yes, status=no, toolbar=no, location=no, scrollbars=yes, directories=no" );
	win.focus();
}
//-->
</script>
</head>
<body>
<iframe src="menu.jsp" width="100%" height="55" frameborder="0" marginwidth="0" scrolling="no" title=""></iframe>
<h2>Record List</h2>
<%
	//----------------------------------------------------
	// リクエストパラメータ取得
	//----------------------------------------------------
	request.setCharacterEncoding("utf-8");
	String act = "";
	String selDbName = "";
	String[] recIds = null;
	Enumeration<String> names = (Enumeration<String>)request.getParameterNames();
	while ( names.hasMoreElements() ) {
		String key = (String)names.nextElement();
		if ( key.equals("act") ) {
			act = request.getParameter(key);
		}
		else if ( key.equals("db") ) {
			selDbName = request.getParameter(key);
		}
		else if ( key.equals("id") ) {
			recIds = request.getParameterValues(key);
		}
	}
	
	//----------------------------------------------------
	// 各種パラメータを取得
	//----------------------------------------------------
	final String reqUrl = request.getRequestURL().toString();
	final String baseUrl = reqUrl.substring( 0, (reqUrl.indexOf("/mbadmin") + 1 ) );
	final String realPath = application.getRealPath("/");
	AdminCommon admin = new AdminCommon(reqUrl, realPath);
	final String dbRootPath = admin.getDbRootPath();
	final String dbHostName = admin.getDbHostName();
	GetConfig conf = new GetConfig(baseUrl);
	final String outPath = (!admin.getOutPath().equals("")) ? admin.getOutPath() : FileUpload.UPLOAD_PATH;
	final String tmpPath = (new File(outPath + "/" + sdf.format(new Date()))).getPath() + File.separator;
	final String backupPath = tmpPath + "backup" + File.separator;
	DatabaseAccess db = null;
	boolean isResult = true;
	OperationManager om = OperationManager.getInstance();
	
	try {
		//----------------------------------------------------
		// 存在するDB名を取得
		//----------------------------------------------------
		List dbNameList = Arrays.asList(conf.getDbName());
		ArrayList<String> dbNames = new ArrayList<String>();
		dbNames.add("");
		File[] dbDirs = (new File( dbRootPath )).listFiles();
		if ( dbDirs != null ) {
			for ( File dbDir : dbDirs ) {
				if ( dbDir.isDirectory() ) {
					int pos = dbDir.getName().lastIndexOf("\\");
					String dbDirName = dbDir.getName().substring( pos + 1 );
					pos = dbDirName.lastIndexOf("/");
					dbDirName = dbDirName.substring( pos + 1 );
					if (dbNameList.contains(dbDirName)) {
						//DBディレクトリが存在し、massbank.confに記述があるDBのみ有効とする
						dbNames.add( dbDirName );
					}
				}
			}
		}
		if (dbDirs == null || dbNames.size() == 0) {
			out.println( msgErr( "[" + dbRootPath + "]&nbsp;&nbsp;directory not exist." ) );
			return;
		}
		Collections.sort(dbNames);
		
		//----------------------------------------------------
		// レコードパス＆URL設定
		//----------------------------------------------------
		if ( selDbName.equals("") || !dbNames.contains(selDbName) ) {
			selDbName = dbNames.get(0);
		}
		final String recPath = (new File(dbRootPath + "/" + selDbName)).getPath();
		if ( !(new File(recPath)).isDirectory() ) {
			out.println( msgErr( "[" + recPath + "]&nbsp;&nbsp;directory not exist." ) );
			return;
		}
		String recUrl = baseUrl + "jsp/FwdRecord.jsp?id=";
		
		//----------------------------------------------------
		// フォーム表示
		//----------------------------------------------------
		out.println( "<form name=\"formMain\" action=\"" + reqUrl + "\" method=\"post\" onSubmit=\"doWait();\">" );
		out.println( "<input type=\"hidden\" name=\"act\" value=\"get\">" );
		out.println( "<span class=\"baseFont\">Database :</span>&nbsp;" );
		out.println( "<select name=\"db\" class=\"db\" onChange=\"selDb();\">" );
		for ( int i=0; i<dbNames.size(); i++ ) {
			String dbName = dbNames.get(i);
			out.print( "<option value=\"" + dbName + "\"" );
			if ( dbName.equals(selDbName) ) {
				out.print( " selected" );
			}
			if ( i == 0 ) {
				out.println( ">------------------</option>" );
			}
			else {
				out.println( ">" + dbName + "</option>" );
			}
		}
		out.println( "</select>" );
		out.println( "<input type=\"submit\" value=\"Get\" onClick=\"return checkDb();\">" );
		out.println( "</form>" );
		out.println( "<hr><br>" );
		
		//----------------------------------------------------
		// DB接続
		//----------------------------------------------------
		if ( act.equals("get") || act.equals("del") ) {
			db = new DatabaseAccess(dbHostName, selDbName);
			isResult = db.open();
			if ( !isResult ) {
				out.println( msgErr( "not connect to database.") );
				return;
			}
		}
		
		//----------------------------------------------------
		// レコード一覧表示
		//----------------------------------------------------
		if ( act.equals("get") ) {
			isResult = om.startOparation(om.P_RECORD, om.TP_VIEW, selDbName);
			if ( !isResult ) {
				out.println( msgWarn( "other users are updating. please access later. ") );
				return;
			}
			isResult = dispRecord( db, out, reqUrl, selDbName, recPath, recUrl );
		}
		//----------------------------------------------------
		// レコード削除
		//----------------------------------------------------
		else if ( act.equals("del") ) {
			isResult = om.startOparation(om.P_RECORD, om.TP_UPDATE, selDbName);
			if ( !isResult ) {
				out.println( msgWarn( "other users are updating. please access later. ") );
				return;
			}
			
			// 作業用ディレクトリ作成
			(new File(tmpPath)).mkdir();
			(new File(backupPath)).mkdir();
			String os = System.getProperty("os.name");
			if(os.indexOf("Windows") == -1){
				isResult = FileUtil.changeMode("777", tmpPath);
				if ( !isResult ) {
					out.println( msgErr( "[" + tmpPath + "]&nbsp;&nbsp; chmod failed.") );
					return;
				}
			}
			
			//---------------------------------------------
			// ロールバック用DBダンプ処理
			//---------------------------------------------
			// SQLダンプ
			String dumpPath = tmpPath + selDbName + ".dump";
			String[] tables = new String[]{tableInfo[0][0], tableInfo[1][0], tableInfo[2][0], tableInfo[3][0], tableInfo[4][0], tableInfo[5][0]};
			isResult = FileUtil.execSqlDump(dbHostName, selDbName, tables, dumpPath);
			if ( !isResult ) {
				Logger.global.severe( "sqldump failed." + NEW_LINE +
				                      "    dump file : " + dumpPath );
				out.println( msgErr( "db dump failed." ) );
				out.println( msgInfo( "0 record delete." ) );
				return;
			}
			// ファイルダンプ
			File srcFile = null;
			File destFile = null;
			try {
				for ( String recId : recIds ) {
					srcFile = new File(recPath + File.separator + recId + ".txt");
					destFile = new File(backupPath + recId + ".txt");
					if ( srcFile.isFile() ) {
						FileUtils.copyFile(srcFile, destFile);
					}
				}
			} catch (IOException e) {
				Logger.global.severe( "file copy failed." + NEW_LINE +
				                      "    " + srcFile + " to " + destFile );
				e.printStackTrace();
				out.println( msgErr( "file dump failed." ) );
				out.println( msgInfo( "0 record delete." ) );
				return;
			}
			
			// レコード＆各テーブル削除
			int delNum = delRecord( db, out, conf, dbHostName, recIds, recPath, tmpPath, selDbName );
			if ( delNum >= 0 ) {
				NumberFormat nf = NumberFormat.getNumberInstance();
				out.println( msgInfo( nf.format(recIds.length) + " record delete." ) );
			}
			else {
				//---------------------------------------------
				// ロールバック処理
				//---------------------------------------------
				// ファイルロールバック
				try {
					for ( String recId : recIds ) {
						srcFile = new File(backupPath + recId + ".txt");
						destFile = new File(recPath + File.separator + recId + ".txt");
						if ( srcFile.isFile() ) {
							FileUtils.copyFile(srcFile, destFile);
						}
					}
				}
				catch (IOException e) {
					Logger.global.severe( "rollback(file delete) failed." );
					e.printStackTrace();
					out.println( msgErr( "rollback failed." ) );
				}
				// SQLロールバック
				isResult = FileUtil.execSqlFile(dbHostName, selDbName, dumpPath);
				if ( !isResult ) {
					Logger.global.severe( "rollback(sqldump) failed." );
					out.println( msgErr( "rollback failed." ) );
				}
				out.println( msgInfo( "0 record delete." ) );
			}
			
			//----------------------------------------------------
			// ヒープテーブル更新
			//----------------------------------------------------
			final String cgiUrl = baseUrl + "cgi-bin/CreateHeap.cgi";
			final String cgiParam = "dsn=" + selDbName;
			boolean tmpRet = execCgi( cgiUrl, cgiParam );
			if ( !tmpRet ) {
				Logger.global.severe( "cgi execute failed." + NEW_LINE +
				                      "    url : " + cgiUrl + NEW_LINE +
				                      "    param : " + cgiParam );
			}
		}
	}
	finally {
		if ( db != null ) {
			db.close();
		}
		if ( act.equals("get") ) {
			om.endOparation(om.P_RECORD, om.TP_VIEW, selDbName);
		}
		else if ( act.equals("del") ) {
			File tmpDir = new File(tmpPath);
			if (tmpDir.exists()) {
				FileUtil.removeDir( tmpDir.getPath() );
			}
			om.endOparation(om.P_RECORD, om.TP_UPDATE, selDbName);
			out.println( msgInfo( "Done." ) );
		}
		out.println( "</body>" );
		out.println( "</html>" );
	}
%>
