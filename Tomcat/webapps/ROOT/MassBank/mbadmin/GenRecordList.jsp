<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%
/*******************************************************************************
 *
 * Copyright (C) 2008 JST-BIRD MassBank
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
 * 化合物名リスト作成処理
 *
 * ver 1.0.2 2009.02.02
 *
 ******************************************************************************/
%>

<%@ page import="java.net.*, java.io.*, java.util.*" %>
<%@ page import="org.apache.poi.hssf.usermodel.*" %>
<%@ page import="org.apache.poi.hssf.util.*" %>
<%@ page import="massbank.GetConfig" %>
<%@ page import="massbank.MassBankCommon" %>
<%@ page import="massbank.admin.AdminCommon" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Content-Style-Type" content="text/css">
<link rel="stylesheet" type="text/css" href="css/admin.css">
<title>Record List Generator</title>
</head>
<body>
<iframe src="menu.jsp" width="100%" height="55" frameborder="0" marginwidth="0" scrolling="no" title=""></iframe>
<h2>Record List Generator</h2>
<%
	// ベースUrl, JSP名をセット
	String reqUrl = request.getRequestURL().toString();
	String find = "mbadmin/";
	int pos1 = reqUrl.indexOf( find );
	String baseUrl = reqUrl.substring( 0, pos1  );
	String jspName = reqUrl.substring( pos1 + find.length() );
	
	// 環境設定ファイルからURLリスト、DB名リストを取得
	GetConfig conf = new GetConfig(baseUrl);
	String[] urlList = conf.getSiteUrl();
	String[] siteNameList = conf.getSiteName();
	String[] dbNameList = conf.getDbName();
	String serverUrl = conf.getServerUrl();
	
	// リクエストパラメータ取得
	int siteNum = 0;
	if ( request.getParameter("site_num") != null ) {
		siteNum = Integer.parseInt(request.getParameter("site_num"));
	}
	
	String act = "";
	if ( request.getParameter("act") != null ) {
		act = request.getParameter("act");
	}
	
	if ( !act.equals("") ) {
		// CGI経由で化合物名リストを取得
		String strUrl = serverUrl + "jsp/" + MassBankCommon.DISPATCHER_NAME;
		String typeName = MassBankCommon.CGI_TBL[MassBankCommon.CGI_TBL_NUM_TYPE][MassBankCommon.CGI_TBL_TYPE_GETLIST];
		String param = "type=" + typeName + "&site=" + String.valueOf(siteNum);
		URL url = new URL( strUrl );
		URLConnection con = url.openConnection();
		con.setDoOutput(true);
		PrintStream psm = new PrintStream( con.getOutputStream() );
		psm.print( param );
		BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
		String line = "";
		ArrayList<String> list = new ArrayList();
		while ( ( line = in.readLine() ) != null ) {
			list.add( line );
		}
		in.close();
		
		// ワークブック、ワークシートを作成
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("list of compounds");
		
		// 見出し用スタイルをセット
		HSSFCellStyle style1 = wb.createCellStyle();
			// フォント - 太字、白色
		HSSFFont font1 = wb.createFont();
		font1.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font1.setColor(HSSFColor.WHITE.index);
		style1.setFont(font1);
			// 背景色
		style1.setFillForegroundColor(HSSFColor.GREY_40_PERCENT.index);
		style1.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		
		// リンク用スタイルをセット
		HSSFCellStyle style2 = wb.createCellStyle();
			// フォント - 青字で下線アリ
		HSSFFont font2 = wb.createFont();
		font2.setColor(HSSFColor.BLUE.index);
		font2.setUnderline(HSSFFont.U_SINGLE);
		style2.setFont(font2);
		style2.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		
		// コメント用
		HSSFPatriarch patr = sheet.createDrawingPatriarch();
		HSSFFont font3 = wb.createFont();
		font3.setFontHeight((short)(20*9));	// 9px
		
		// 化合物リストを書き込む
		HSSFRow hsRow = null;
		int colspan = 0;
		for ( int row = 0; row < list.size(); row++ ){
			line = list.get(row);
			String[] item = line.split("\t");
			hsRow = sheet.createRow(row + 1);
			
			// 化合物名、Formnulaをセット
			for ( short col = 0; col < item.length - 1; col++ ) {
				HSSFCell cell = hsRow.createCell(col);
				String val = item[col];
				cell.setCellValue(val);
			}
			
			// Recored IDをセット
			boolean isColReSize = false;
			String partId = item[item.length - 1];
			String[] idList = partId.split("@");
			if ( idList.length > colspan ) {
				isColReSize = true;
				colspan = idList.length;
			}
			for ( short n = 0; n < idList.length; n++ ) {
				short col2 = (short)(item.length - 1 + n);
				HSSFCell cell = hsRow.createCell(col2);
				int pos = idList[n].indexOf(" NAME=");
				String id = idList[n].substring( 0, pos );
				String recordName = idList[n].substring( pos + 6 );
				
				cell.setCellValue(id);
				if ( isColReSize ) {
					sheet.autoSizeColumn(col2);
				}
				// ハイパーリンクをセット
				String linkUrl = serverUrl + "jsp/Dispatcher.jsp?type=disp&id="
												 + id + "&site=" + String.valueOf(siteNum);
				cell.setCellFormula("HYPERLINK(\"" + linkUrl + "\",\"" + id + "\")");
				
				// スタイルをセット
				cell.setCellStyle(style2);
				
				// コメントをセット
				HSSFComment comment = patr.createComment(new HSSFClientAnchor(0,0,0,150, (short)0,0,(short)2,1)); 
				cell.setCellComment(comment);
				HSSFRichTextString richText = new HSSFRichTextString(recordName);
				richText.applyFont(font3);
				comment.setString(richText);
			}
		}
		
		//** 見出しを書き込む
		hsRow = sheet.createRow(0);
		String[] headline = { "Compound Name", "Formula", "Recored ID" };
		for ( short col = 0; col < headline.length; col++ ){
				HSSFCell cell = hsRow.createCell(col);
				cell.setCellValue(headline[col]);
				cell.setCellStyle(style1);
				sheet.autoSizeColumn(col);
		}
		// Recored IDヘッダのセルを結合
		sheet.addMergedRegion( new Region(0,(short)(headline.length-1), 0,(short)(colspan+1)) );
		
		//** 保存
		String outPath = System.getProperty("catalina.home") + "/webapps/ROOT/";
		int pos = baseUrl.lastIndexOf("MassBank");
		if ( pos >= 0 ) {
			outPath += baseUrl.substring(pos);
		}
		outPath += "temp/";
		String fileName = siteNameList[siteNum].replace("Univ.","").trim().toLowerCase();
		String xslPath = outPath + fileName + "_list.xls";
		FileOutputStream fso = new FileOutputStream(xslPath);
		wb.write(fso);
		fso.close();
		
		out.println( "<font color=\"blue\"><b>Generate " + xslPath + "</b></font><br><br>" );
	}

%>
<form name="form1" method="post" action="<%out.print(jspName);%>">
<b>Contributor :</b>&nbsp;
<select name="site_num">
<%
	for ( int i = 0; i < dbNameList.length; i++ ) {
		out.print( "<option value=\"" + String.valueOf(i) + "\"" );
		if ( i == siteNum ) {
			out.print( " selected" );
		}
		out.println( ">" + siteNameList[i] );
	}
%>
</select>
<input type="submit" value="Execute">
<input type="hidden" name="act" value="gene">
</form>
</body>
</html>
