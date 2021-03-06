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
 * Multiple Spectra Display アプレット
 *
 * ver 2.0.11 2011.07.28
 *
 ******************************************************************************/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

import massbank.GetConfig;
import massbank.MassBankCommon;

/**
 * スペクトル表示 クラス
 */
public class DisplayAll extends JApplet
{
	private static final long serialVersionUID = 1L;
	private int massRangeMax = 0;
	static int INTENSITY_MAX = 1000;
	static int MARGIN = 15;
	static int MIN_RANGE = 5;
	static int DEF_EX_PANE_SIZE = 150;

	double massStart = 0;
	double massRange = 0;
	int intensityRange = INTENSITY_MAX;
	boolean head2tail = false;
	boolean underDrag = false;
	Point fromPos = null;
	Point toPos = null;
	double xscale = 0;
	JSplitPane jsp_plt2ext = null;
	boolean isMZFlag = false;
	boolean isMassDiff = false;
	int numSpct;
	PlotPane[] plotPane;
	Peak[] peaks1 = null;
	ButtonPane[] buttonPane;
	private String baseUrl;
	private HitPeaks hitPeaks = new HitPeaks();
	private String reqType = "";
	private String[] precursor = null;
	private String searchParam = "";
	private String paramMz  = "";
	private String paramTol = "";
	private String paramInt = "";
	private RecordInfo[] info = null;
	private int[] cnt = null;
	private String[] urlList = null;
	private String serverUrl = "";

	/**
	 * 
	 */
	class PlotPane extends JPanel implements MouseListener,
			MouseMotionListener
	{
		private static final long serialVersionUID = 1L;
		private Timer timer = null;
		private int idPeak;
		private long lastClickedTime = 0;						// 最後にクリックした時間

		/**
		 * 拡大処理をアニメーション化するクラス
		 */
		class AnimationTimer implements ActionListener
		{
			final int LOOP = 15;
			int loopCoef;
			int minx;
			int width;
			double tmpMassStart;
			double tmpMassRange;
			int tmpIntensityRange;
			int movex;

			/**
			 * 
			 */
			public AnimationTimer(int w, int x)
			{
				loopCoef = 0;
				minx = x;
				width = w;
				movex = 0 + MARGIN;
				// 目的拡大率を算出
				double xs = (getWidth() - 2.0d * MARGIN)
						/ massRange;
				tmpMassStart = massStart
						+ ((minx - MARGIN) / xs);
				tmpMassRange = 10 * (width / (10 * xs));
				if (tmpMassRange < MIN_RANGE) {
					tmpMassRange = MIN_RANGE;
				}

				// Intensityのレンジを設定
				if (massRange <= massRangeMax)
				{
					// 最大値を検出。
					int max = 0;
					double start = Math.max(tmpMassStart, 0.0d);
					for (int i=0; i<peaks1.length; i++) {
						if (max < peaks1[i].getMaxIntensity(start, start + tmpMassRange)) {
							max = peaks1[i].getMaxIntensity(start, start + tmpMassRange);
						}
					}
					// 50単位に変換してスケールを決定
					tmpIntensityRange = (int)((1.0d + max / 50.0d) * 50.0d);
					if(tmpIntensityRange > INTENSITY_MAX)
						tmpIntensityRange = INTENSITY_MAX;
				}
			}

			/**
			 * 
			 */
			public void actionPerformed(ActionEvent e)
			{
				xscale = (getWidth() - 2.0d * MARGIN) / massRange;
				int xpos = (movex + minx) / 2;
				if (Math.abs(massStart - tmpMassStart) <= 2
						&& Math.abs(massRange - tmpMassRange) <= 2)
				{
					xpos = minx;
					massStart = tmpMassStart;
					massRange = tmpMassRange;
					timer.stop();
					DisplayAll.this.repaint();
				} else {
					loopCoef++;
					massStart = massStart
							+ (((tmpMassStart + massStart) / 2 - massStart)
									* loopCoef / LOOP);
					massRange = massRange
							+ (((tmpMassRange + massRange) / 2 - massRange)
									* loopCoef / LOOP);
					intensityRange = intensityRange
							+ (((tmpIntensityRange + intensityRange) / 2 - intensityRange)
									* loopCoef / LOOP);
					if (loopCoef >= LOOP)
					{
						movex = xpos;
						loopCoef = 0;
					}
				}
				repaint();
			}
		}

		/**
		 * 
		 */
		public PlotPane(int id)
		{
			idPeak = id;
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		/**
		 * 
		 */
		int setStep(int range)
		{
			if (range < 20)
				return 2;
			if (range < 50)
				return 5;
			if (range < 100)
				return 10;
			if (range < 250)
				return 25;
			if (range < 500)
				return 50;
			return 100;
		}

		/**
		 * (非 Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			int width = getWidth();
			int height = getHeight();
			xscale = (width - 2.0d * MARGIN) / massRange;
			
			ArrayList<Double> mz1Ary = null;
			ArrayList<Double> mz2Ary = null;
			ArrayList<Integer> colorAry = null;

			// ピークバー色づけするカラーをセット
			int colorTblNum = 1;
			Color[] colorTbl = {
					new Color(0xD2,0x69,0x48), new Color(0x22,0x8B,0x22),
					new Color(0x41,0x69,0xE1), new Color(0xBD,0x00,0x8B),
					new Color(0x80,0x80,0x00), new Color(0x8B,0x45,0x13	),
					new Color(0x9A,0xCD,0x32)
			};

			// ヒットしたピークを取得
			if ( reqType.equals("peak") || reqType.equals("diff") ) {
				mz1Ary = hitPeaks.getMz1(idPeak);
				mz2Ary = hitPeaks.getMz2(idPeak);
				colorAry = hitPeaks.getBarColor(idPeak);
			}

			//上部余白のサイズをセット
			int marginTop = 0;
			if ( reqType.equals("diff") ) {
				marginTop = 70;
			}
			else {
				marginTop = MARGIN;
			}
			
			double yscale = (height - (double)(MARGIN +marginTop) ) / intensityRange;
			
			// 背景を白にする
			g.setColor(Color.white);
			g.fillRect(0, 0, width, height);

			g.setFont(g.getFont().deriveFont(9.0f));
			g.setColor(Color.lightGray);

			//========================================================
			// 目盛りを描画
			//========================================================
			g.drawLine(MARGIN, marginTop, MARGIN, height - MARGIN);
			g.drawLine(MARGIN, height - MARGIN, width - MARGIN, height - MARGIN);

			// x軸
			int step = setStep((int)massRange);
			int start = (step - (int)massStart % step) % step;
			for (int i = start; i < (int)massRange; i += step) {
				g.drawLine(MARGIN + (int) (i * xscale),
						height - MARGIN, MARGIN + (int) (i * xscale),
						height - MARGIN + 2);
				g.drawString(formatMass(i + massStart, true),
						MARGIN + (int) (i * xscale) - 5,
						height - 1);
			}

			// y軸
			for (int i = 0; i <= intensityRange; i += intensityRange / 5) {
				g.drawLine(MARGIN - 2, height - MARGIN - (int) (i * yscale),
							MARGIN,	height - MARGIN - (int) (i * yscale));
				g.drawString(String.valueOf(i),	0, height - MARGIN - (int) (i * yscale));
			}

			// ピークがない場合
			if ( peaks1[idPeak].getMZ(0) == 0 ) {
				g.setFont( new Font("Arial", Font.ITALIC, 24) );
				g.setColor( Color.LIGHT_GRAY );
				g.drawString( "No peak was observed.",	width/2-110, height / 2 );
				return;
			}

			double hitMz1Prev = 0;
			double hitMz2Prev = 0;
			int hitMz1Cnt = -1;
			int hitMz2Cnt = -1;
			
			//========================================================
			// ピークバーを描画
			//========================================================
			g.setColor(Color.black);
			int end, its, x, w;
			double peak;
			String baseDiff = peaks1[idPeak].getBase();
			String msDiff;
			start = peaks1[idPeak].getIndex(massStart);
			end = peaks1[idPeak].getIndex(massStart + massRange);
			for (int i = start; i < end; i++) {
				peak = peaks1[idPeak].getMZ(i);
				its = peaks1[idPeak].getIntensity(i);
				msDiff = peaks1[idPeak].getDiff(i);
				x = MARGIN + (int) ((peak - massStart) * xscale) - (int) Math.floor(xscale / 8);
				w = (int) (xscale / 8);
				if(MARGIN >= x){
					w = w - (MARGIN - x);
					x = MARGIN;
				}
				if ( w < 2 ) {
					w = 2;
				}
				
				boolean isHit = false;
				// 差表示境界線描画
				if ( isMassDiff && baseDiff != null) {
					g.setColor(Color.magenta);
				}
				// PeakSearch、PeakDifferenceSearchの場合はヒットピークに色づけする
				else if ( reqType.equals("peak") || reqType.equals("diff") ) {
					int j = 0;
					double mz = 0;
					
					// ヒットピークmz1と一致しているか
					for ( j = 0; j < mz1Ary.size(); j++ ) {
						mz = mz1Ary.get(j);
						if ( peak == mz ) {
							isHit = true;
							if ( mz - hitMz1Prev >= 1 ) {
								hitMz1Cnt++;
							}
							
							if ( colorAry.get(j) != null ) {
								colorTblNum = colorAry.get(j);
							}
							else {
								colorTblNum = hitMz1Cnt - (hitMz1Cnt / colorTbl.length) * colorTbl.length;
								hitPeaks.setBarColor( idPeak, j, colorTblNum );
							}
							hitMz1Prev = mz;
							break;
						}
					}
					// mz1と不一致の場合、ヒットピークmz2と一致しているか
					if ( !isHit ) {
						for ( j = 0; j < mz2Ary.size(); j++ ) {
							mz = mz2Ary.get(j);
							if ( peak == mz ) {
								isHit = true;
								
								if ( colorAry.get(j) != null ) {
									colorTblNum = colorAry.get(j);
								}
								else if ( mz - hitMz2Prev >= 1 ) {
									colorTblNum = hitMz2Cnt - (hitMz2Cnt / colorTbl.length) * colorTbl.length;
									hitPeaks.setBarColor( idPeak, j, colorTblNum );
								}
								else {}
								hitMz2Cnt++;
								hitMz2Prev = mz;
								break;
							}
						}
					}
					// ヒットピークと一致している場合、描画色をセット
					if ( isHit ) {
						g.setColor( colorTbl[colorTblNum] );
					}
				}

				// ピークバーを描画
				g.fill3DRect(x,	height - MARGIN - (int) (its * yscale),
								w, (int)(its * yscale), true);
				
				// 差を描画
				if ( isMassDiff && baseDiff != null ) {
					String sign = "";
					if ( Double.parseDouble(msDiff) > 0 ) {
						sign = "+";
					}
					g.setColor(Color.magenta);
					g.drawString(sign + msDiff,
							x, height - MARGIN - (int) (its * yscale));
				}
				// m/z値を描画
				else if ( its > intensityRange * 0.4 || isMZFlag || isHit ) {
					if ( isMZFlag && its > intensityRange * 0.4 ) {
						g.setColor(Color.red);
					}
					else if ( isHit ) {
					}
					else {
						g.setColor(Color.black);
					}
					g.drawString(formatMass(peak, false),
								x, height - MARGIN - (int) (its * yscale));
				}
				g.setColor(Color.black);
			}
			
			//========================================================
			// ピーク差検索でヒットしたピークの位置を表示
			//========================================================
			if ( !isMassDiff && baseDiff != null && reqType.equals("diff") ) {
				String[] diffmzs = hitPeaks.getDiffMz(idPeak);
				String diffmz = diffmzs[ hitPeaks.getListNum()-1 ];
				int pos = diffmz.indexOf(".");
				if ( pos > 0 ) {
					BigDecimal bgMzZDiff = new BigDecimal( diffmz );
					diffmz = (bgMzZDiff.setScale(1, BigDecimal.ROUND_DOWN)).toString(); 
				}

				double mz1Prev = 0;
				int hitCnt = 0;
				for ( int j = 0; j < mz1Ary.size(); j++ ) {
					double mz1 = mz1Ary.get(j);
					double mz2 = mz2Ary.get(j);
					if ( mz1 - mz1Prev >= 1 ) {
						g.setColor(Color.GRAY);

						/* ピークバーのライン幅 */
						int barWidth = (int)Math.floor(xscale / 8);
						/* 横線左の開始位置 */
						int x1 = MARGIN + (int)((mz1 - massStart) * xscale) - barWidth/2;
						/* 横線右の開始位置 */
						int x2 = MARGIN + (int)((mz2 - massStart) * xscale) - barWidth/2;
						/* 横線右の開始位置 */
						int xc = x1 + (x2-x1) / 2 - 12;
						/* Ｙ座標 */
						int y = height - MARGIN - (int)( (1035 * yscale) + (++hitCnt * 12) );
						/* 文字幅 */
						int xm = (int)(diffmz.length() * 5)+4;

						int padding = 5;

						// 横線描画
						g.drawLine( x1,y , xc,y );
						g.drawLine( xc + xm + padding,y, x2,y );
						// 縦線描画
						g.drawLine( x1,y, x1,y+4 );
						g.drawLine( x2,y, x2,y+4 );

						// ピーク差の値を描画
						colorTblNum = colorAry.get(j);
						g.setColor( colorTbl[colorTblNum] );
						g.fillRect( xc, y - padding, (xc + xm + padding) - xc, padding*2 );
						g.setColor( Color.WHITE );
						g.drawString( diffmz, xc + padding , y+3 );
					}
					mz1Prev = mz1;
				}
			}
			
			// プレカーサーm/zにマーク付け
			if ( !precursor[idPeak].equals("") ) {
				int pre = Integer.parseInt(precursor[idPeak]);
				int xPre = MARGIN + (int)((pre - massStart) * xscale) - (int)Math.floor(xscale / 8);
				double pw = xscale / 8;
				if(MARGIN >= xPre){
					pw = pw - (MARGIN - xPre);
					xPre = MARGIN;
				}
				if ( pw < 2.0 ) {
					pw = 2.0;
				}
				xPre += new BigDecimal(pw).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
				int align = new BigDecimal(2.0 / step * 2.0).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
				if ( align < 1 ) {
					align = 1;
				}
				xPre -= align;
				int yPre = height - MARGIN;
				
				// プリカーサーm/zがグラフ内の場合のみ描画
				if (xPre >= MARGIN && xPre <= width - MARGIN) {
					int [] xp = { xPre, xPre + 6, xPre - 6 };
					int [] yp = { yPre, yPre + 6, yPre + 6 };
					g.setColor( Color.RED );
					g.fillPolygon( xp, yp, xp.length );
				}
			}
			
			// 差表示境界描画
			if ( isMassDiff && baseDiff != null) {
				
				if (Double.parseDouble(baseDiff) > massStart && Double.parseDouble(baseDiff) <= massStart + massRange) {
					g.setColor(Color.black);
					int bx = MARGIN + (int)((Double.parseDouble(baseDiff) - massStart) * xscale) - (int)Math.floor(xscale / 8);
					double bw = xscale / 8;
					if(MARGIN >= bx){
						bw = bw - (MARGIN - bx);
						bx = MARGIN;
					}
					if ( bw < 2.0 ) {
						bw = 2.0;
					}
					bx += new BigDecimal(bw).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
					int align = new BigDecimal(2.0 / step * 2.0).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
					if ( align < 1 ) {
						align = 1;
					}
					bx -= align;
					
					// 基準値描画
				    g.setFont(g.getFont().deriveFont(14.0f));
					g.drawString(peaks1[idPeak].getBase(), bx, MARGIN);
					g.setFont(g.getFont().deriveFont(9.0f));
					
					// 境界線描画
				    Graphics2D g2 = (Graphics2D)g;
				    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f}, 0.0f));
				    g2.draw(new Line2D.Float(bx, height, bx, MARGIN));
				}
			}

			if (underDrag)
			{// マウスでドラッグした領域を黄色い線で囲む
				g.setXORMode(Color.white);
				g.setColor(Color.yellow);
				int xpos = Math.min(fromPos.x, toPos.x);
				width = Math.abs(fromPos.x - toPos.x);
				g.fillRect(xpos, 0, width, height - MARGIN);
				g.setPaintMode();
			}
		}

		/**
		 * 
		 */
		public void mousePressed(MouseEvent e)
		{
			// 左ボタンの場合
			if ( SwingUtilities.isLeftMouseButton(e) ) {

				if(timer != null && timer.isRunning())
					return;
	
				fromPos = toPos = e.getPoint();
			}
		}

		/**
		 * 
		 */
		public void mouseDragged(MouseEvent e)
		{
			// 左ボタンの場合
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				if(timer != null && timer.isRunning())
					return;
	
				underDrag = true;
				toPos = e.getPoint();
				repaint();
			}
		}

		/**
		 * 
		 */
		public void mouseReleased(MouseEvent e)
		{
			// 左ボタンの場合
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				if (!underDrag || (timer != null && timer.isRunning()))
					return;
				underDrag = false;
				if ((fromPos != null) && (toPos != null)) {
					if (Math.min(fromPos.x, toPos.x) < 0)
						massStart = Math.max(0, massStart - massRange / 3);
	
					else if (Math.max(fromPos.x, toPos.x) > getWidth())
						massStart = Math.min(massRangeMax - massRange, massStart + massRange / 3);
					else {
						if (peaks1 != null) {
							timer = new Timer(30,
									new AnimationTimer(Math.abs(fromPos.x - toPos.x),
											Math.min(fromPos.x, toPos.x)));
							timer.start();
						} else {
							fromPos = toPos = null;
							repaint();
						}
					}
				}
			}
		}

		/**
		 * 
		 */
		public void mouseClicked(MouseEvent e)
		{
			// 左ボタンの場合
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				
				// クリック間隔算出
				long interSec = (e.getWhen() - lastClickedTime);
				lastClickedTime = e.getWhen();
				
				// ダブルクリックの場合（クリック間隔280ミリ秒以内）
				if(interSec <= 280){
					
					// 拡大処理
					fromPos = toPos = null;
					initMass();
					repaint();
				}
			}
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}
		
		
		/**
		 * m/zの表示用フォーマット
		 * 画面表示用にm/zの桁数を合わせて返却する
		 * @param mass フォーマット対象のm/z
		 * @param isForce 桁数強制統一フラグ（true:0埋めと切捨てを行う、false:切捨てのみ行う）
		 * @return フォーマット後のm/z
		 */
		private String formatMass(double mass, boolean isForce) {
			final int ZERO_DIGIT = 4;
			String massStr = String.valueOf(mass);
			if (isForce) {
				// 強制的に全ての桁を統一する（0埋めと切捨てを行う）
				if (massStr.indexOf(".") == -1) {
					massStr += ".0000";
				}
				else {
					if (massStr.indexOf(".") != -1) {
						String [] tmpMzStr = massStr.split("\\.");
						if (tmpMzStr[1].length() <= ZERO_DIGIT) {
							int addZeroCnt = ZERO_DIGIT - tmpMzStr[1].length();
							for (int j=0; j<addZeroCnt; j++) {
								massStr += "0";
							}
						}
						else {
							if (tmpMzStr[1].length() > ZERO_DIGIT) {
								massStr = tmpMzStr[0] + "." + tmpMzStr[1].substring(0, ZERO_DIGIT);
							}
						}
					}
				}
			}
			else {
				// 桁を超える場合のみ桁を統一する（切捨てのみ行う）
				if (massStr.indexOf(".") != -1) {
					String [] tmpMzStr = massStr.split("\\.");
					if (tmpMzStr[1].length() > ZERO_DIGIT) {
						massStr = tmpMzStr[0] + "." + tmpMzStr[1].substring(0, ZERO_DIGIT);
					}
				}
			}
			return massStr;
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("serial")
	class ButtonPane extends JPanel implements ActionListener {
		
		JToggleButton mzDisp = null;
		JToggleButton msDiff = null;
		private String comNameDiff = "show_diff";
		private int index;
		
		public ButtonPane(int idx) {
			index = idx;
			
			JButton leftmostB = new JButton("<<");
			leftmostB.setActionCommand("<<");
			leftmostB.addActionListener(this);
			leftmostB.setMargin(new Insets(0, 0, 0, 0));

			JButton leftB = new JButton(" < ");
			leftB.setActionCommand("<");
			leftB.addActionListener(this);
			leftB.setMargin(new Insets(0, 0, 0, 0));

			JButton rightB = new JButton(" > ");
			rightB.setActionCommand(">");
			rightB.addActionListener(this);
			rightB.setMargin(new Insets(0, 0, 0, 0));

			JButton rightmostB = new JButton(">>");
			rightmostB.setActionCommand(">>");
			rightmostB.addActionListener(this);
			rightmostB.setMargin(new Insets(0, 0, 0, 0));

			mzDisp = new JToggleButton("show all m/z");
			mzDisp.setActionCommand("mz");
			mzDisp.addActionListener(this);
			mzDisp.setMargin(new Insets(0, 0, 0, 0));

			msDiff = new JToggleButton("mass difference");
			msDiff.setActionCommand("msdiff");
			msDiff.addActionListener(this);
			msDiff.setMargin(new Insets(0, 0, 0, 0));
			
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(leftmostB);
			add(leftB);
			add(rightB);
			add(rightmostB);
			add(mzDisp);
			add(msDiff);
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent ae)
		{
			String com = ae.getActionCommand();
			if (com.equals("<<"))
				massStart = Math
						.max(0, massStart - massRange);
			else if (com.equals("<"))
				massStart = Math.max(0, massStart - massRange
						/ 4);
			else if (com.equals(">"))
				massStart = Math.min(massRangeMax - massRange,
						massStart + massRange / 4);
			else if (com.equals(">>"))
				massStart = Math.min(massRangeMax - massRange,
						massStart + massRange);
			else if (com.equals("mz")) {
				isMZFlag = mzDisp.isSelected();
				if ( isMZFlag ) { 
					msDiff.setSelected(false);
					isMassDiff = msDiff.isSelected();
				}
				for (int i=0; i<buttonPane.length; i++) {
					if (i == index) continue;
					buttonPane[i].mzDisp.setSelected(isMZFlag);
					buttonPane[i].msDiff.setSelected(isMassDiff);
				}
			}
			else if (com.equals("msdiff")) {
				isMassDiff = msDiff.isSelected();
				if ( isMassDiff ) {
					mzDisp.setSelected(false);
					isMZFlag = mzDisp.isSelected();
				}
				for (int i=0; i<buttonPane.length; i++) {
					if (i == index) continue;
					buttonPane[i].mzDisp.setSelected(isMZFlag);
					buttonPane[i].msDiff.setSelected(isMassDiff);
				}
			}
			
			// Diffボタン押下時
			int pos = com.indexOf( comNameDiff );
			if ( pos >= 0 ) {
				int num = Integer.parseInt(com.substring(comNameDiff.length()));
				hitPeaks.setListNum(num);
			}
			DisplayAll.this.repaint();
		}
		
		/**
		 * 
		 */
		public void addDiffButton(int idPeak)
		{
			// Diffボタン表示
			if ( reqType.equals("diff") ) {
				String[] diffmzs = hitPeaks.getDiffMz(idPeak);
				JButton[] diffbtn = new JButton[diffmzs.length];
				for ( int i = 0; i < diffmzs.length; i++) {
					diffbtn[i] = new JButton( "Diff." + diffmzs[i] );
					diffbtn[i].setActionCommand( comNameDiff + Integer.toString(i+1) );
					diffbtn[i].addActionListener(this);
					diffbtn[i].setMargin( new Insets(0, 0, 0, 0) );
					add(diffbtn[i]);
				}		
			}
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("serial")
	class NameButton extends JButton implements ActionListener {
		String acc;
		String site;

		public NameButton(String name, String id, String site) {
			JLabel idLabel = new JLabel(id + ":");
			idLabel.setPreferredSize(new Dimension(68, 16));
			add(idLabel);
			JLabel nameLabel = new JLabel(name);
			nameLabel.setPreferredSize(new Dimension(600, 16));
			nameLabel.setForeground(Color.BLUE);
			add(nameLabel);
			
			acc = id;
			this.site = site;
			this.addActionListener(this);
			setPreferredSize(new Dimension(770, getPreferredSize().height));
			setHorizontalAlignment(SwingConstants.LEFT);
			setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		}

		public void actionPerformed(ActionEvent ae) {
			try {
				String typeName = "";
				if ( reqType.equals("diff") ) {
					typeName = MassBankCommon.CGI_TBL[MassBankCommon.CGI_TBL_NUM_TYPE][MassBankCommon.CGI_TBL_TYPE_DISPDIFF];
				}
				else {
					typeName = MassBankCommon.CGI_TBL[MassBankCommon.CGI_TBL_NUM_TYPE][MassBankCommon.CGI_TBL_TYPE_DISP];
				}
				String reqStr = baseUrl + MassBankCommon.DISPATCHER_NAME + "?type=" + typeName + "&id=" + acc + "&site=" + this.site;
				if ( reqType.equals("peak") || reqType.equals("diff") ) {
					reqStr += searchParam;
				}
				DisplayAll.this.getAppletContext().showDocument(new URL(reqStr), "_blank");

			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void init() {
		setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));

		// 環境設定ファイルから連携サイトのURLを取得
		String confPath = getCodeBase().toString();
		confPath = confPath.replaceAll( "jsp/", "" );
		GetConfig conf = new GetConfig(confPath);
		urlList = conf.getSiteUrl();
		serverUrl = conf.getServerUrl();
		baseUrl = serverUrl + "jsp/";

		// ピーク検索、ピーク差検索時のパラメータ取得
		int paramMzNum = 0;
		if ( getParameter("type") != null ) {
			reqType = getParameter("type");
			if ( reqType.equals("peak") || reqType.equals("diff") ) {
				paramMzNum = Integer.parseInt( getParameter("pnum") );

				searchParam = "&num=" + paramMzNum;
				for ( int i = 0; i < paramMzNum; i++ ) {
					String pnum = Integer.toString(i);
					String mz = getParameter( "mz" + pnum );
					String tol = getParameter( "tol" + pnum );
					String rInt = getParameter( "int" + pnum );
					paramMz  += mz  + ",";
					paramTol += tol + ",";
					paramInt += rInt + ",";
					searchParam += "&mz" + pnum + "=" + mz;
					searchParam += "&tol" + pnum + "=" + tol;
					searchParam += "&int" + pnum + "=" + rInt;
				}
			}
		}

		numSpct = Integer.valueOf(getParameter("num"));
		plotPane = new PlotPane[numSpct];
		buttonPane = new ButtonPane[numSpct];
		clear();
		peaks1 = new Peak[numSpct];
		info = new RecordInfo[numSpct];
		cnt = new int[urlList.length];
		HashSet<String> compoundNameList = new HashSet();
		for ( int i = 0; i < numSpct; i++ ) {
			// パラメータ取得
			String pnum = Integer.toString(i+1);
			int siteNo = Integer.parseInt( getParameter( "site" + pnum ) );
			String id = getParameter( "id" + pnum );
			String title = getParameter( "name" + pnum );
			String formula = getParameter( "formula" + pnum );
			String mass = getParameter( "mass" + pnum );
			String ion = getParameter( "ion" + pnum );
			int num = cnt[siteNo]++;
			info[i] = new RecordInfo(id, title, siteNo, num, formula, mass, ion);
			String[] items = title.split(";");
			compoundNameList.add(items[0]);
		}

		// ピークデータ取得
		ArrayList resultList = getPeakData();

		// Molfileデータ取得
		Map<String, String > mapMolData = getMolData(compoundNameList);

		hitPeaks.mzInfoList = new ArrayList[numSpct];
		precursor = new String[numSpct];
		Vector<Vector<String>> mzAry = new Vector<Vector<String>>();
		for ( int i = 0; i < numSpct; i++ ) {
			int siteNo = info[i].getSiteNo();
			int num = info[i].getNumber();
			ArrayList result = (ArrayList)resultList.get(siteNo);
			String line = (String)result.get(num);

			// ピーク検索、ピーク差検索から呼ばれた場合、ヒットしたm/z値が返るので格納する
			String findStr = "hit=";
			int pos = line.indexOf( findStr );
			if ( pos > 0 ) { 
				String hit = line.substring( pos + 4 );
				String[] hitMzInfo = hit.split("\t");
				
				boolean isDiff = false;
				// ピーク検索の場合
				if ( reqType.equals("diff") ) {
					isDiff = true;
				}
				// m/z値を格納
				ArrayList mzInfoList = hitPeaks.setMz( hitMzInfo, isDiff );
				hitPeaks.mzInfoList[i] = new ArrayList();
				hitPeaks.mzInfoList[i].addAll(mzInfoList);
				line = line.substring( 0, pos );
			}

			// プレカーサー
			findStr = "precursor=";
			pos = line.indexOf(findStr);
			int posNext = 0;
			if ( pos > 0 ) { 
				posNext = line.indexOf( "\t", pos );
				precursor[i] = line.substring( pos + findStr.length(), posNext );
				line = line.substring( 0, pos );
			}
			else {
				precursor[i] = "";
			}

			String[] tmp = line.split("\t\t");
			Vector<String> mzs = new Vector<String>();

			// m/z格納
			for (int j = 0; j < tmp.length; j++ ) {
				mzs.add( tmp[j] );
			}
			mzAry.add( mzs );
		}

		for ( int i = 0; i < numSpct; i++ ) {
			plotPane[i] = new PlotPane(i);
			plotPane[i].setPreferredSize( new Dimension(780, 200) );
			plotPane[i].repaint(); 

			JPanel pane1 = new JPanel();
			String title = info[i].getTitle();
			String id = info[i].getID();
			String site = String.valueOf(info[i].getSiteNo());
			pane1.add( new NameButton( title, id, site ) );
			pane1.setLayout( new FlowLayout(FlowLayout.LEFT) );
			pane1.setMaximumSize( new Dimension(pane1.getMaximumSize().width, 100) );
			JPanel parentPane = new JPanel();
			JPanel childPane1 = new JPanel();
			JPanel childPane2 = new JPanel();
			childPane2.setBackground(Color.WHITE);
			childPane2.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			childPane2.setMaximumSize(new Dimension(200, parentPane.getMaximumSize().height));
			childPane1.add(pane1);
			childPane1.add(plotPane[i]);

			buttonPane[i] = new ButtonPane(i);
			buttonPane[i].addDiffButton(plotPane[i].idPeak);
			buttonPane[i].setLayout( new FlowLayout(FlowLayout.LEFT, 0, 0) );
			buttonPane[i].setMaximumSize( new Dimension(buttonPane[i].getMaximumSize().width, 100) );
			childPane1.add(buttonPane[i]);
			childPane1.setLayout( new BoxLayout(childPane1, BoxLayout.Y_AXIS) );
			parentPane.add(childPane1);

			//●パネル右上: FORMULAとEXACT MASSを表示
			JLabel formuraLabel1 = new JLabel("    Formula: ");
			formuraLabel1.setOpaque(true);
			formuraLabel1.setBackground(Color.WHITE);
			formuraLabel1.setPreferredSize( new Dimension(84, 17) );
			childPane2.add(formuraLabel1);
			
			JLabel formuraLabel2 = new JLabel(info[i].getFormula());
			formuraLabel2.setOpaque(true);
			formuraLabel2.setForeground(new Color(57, 127, 0));
			formuraLabel2.setBackground(Color.WHITE);
			formuraLabel2.setPreferredSize( new Dimension(116, 17) );
			childPane2.add(formuraLabel2);
			
			JLabel emassLabel1 = new JLabel("    Exact Mass: ");
			emassLabel1.setOpaque(true);
			emassLabel1.setBackground(Color.WHITE);
			emassLabel1.setPreferredSize( new Dimension(84, 17) );
			childPane2.add(emassLabel1);
			
			JLabel emassLabel2 = new JLabel();
			emassLabel2.setOpaque(true);
			emassLabel2.setForeground(new Color(57, 127, 0));
			emassLabel2.setBackground(Color.WHITE);
			emassLabel2.setPreferredSize( new Dimension(116, 17) );
			String emass = info[i].getExactMass();
			if ( !emass.equals("") && !emass.equals("0") ) {
				emassLabel2.setText(emass);
			}
			childPane2.add(emassLabel2);
			
			//●パネル右中: Mol構造表示パネルをセット
			JPanel pane3 = null;
			String[] items = title.split(";");
			boolean isExist = false;
			String compoundName = items[0].toLowerCase();
			if ( mapMolData.containsKey(compoundName) ) {
				String moldata = mapMolData.get(compoundName);
				if ( !moldata.equals("") ) {
					pane3 = (MolViewPaneExt)new MolViewPaneExt(moldata, 200, this);
					isExist = true;
				}
			}
			if ( !isExist ) {
				// データを取得できなかった場合
				JLabel lbl = new JLabel( "Not Available", JLabel.CENTER );
				lbl.setPreferredSize( new Dimension(180, 180) );
				lbl.setBackground(new Color(0xF8,0xF8,0xFF));
				lbl.setBorder( new LineBorder(Color.BLACK, 1) );
				lbl.setOpaque(true);
				GridBagLayout layout = new GridBagLayout();
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets = new Insets(1, 1, 1, 1);
				layout.setConstraints(lbl, gbc);

				pane3 = new JPanel();
				pane3.setPreferredSize( new Dimension(200, 200) );
				pane3.setBackground(Color.WHITE);
				pane3.add(lbl);
			}
			childPane2.add(pane3);

			//●パネル右下: 構造式表示下部の余白
			JLabel lbl3 = new JLabel("");
			lbl3.setPreferredSize( new Dimension(200, 30) );
			childPane2.add(lbl3);
			childPane2.setPreferredSize( new Dimension(200, 260) );
			parentPane.add(childPane2);
			parentPane.setLayout( new BoxLayout(parentPane, BoxLayout.X_AXIS) );
			add(parentPane);

			// 上下スペクトルの区切り
			JPanel spacePane = new JPanel();
			spacePane.setPreferredSize( new Dimension(800, 2) );
			spacePane.setBackground(Color.white);
			JLabel lbl4 = new JLabel("");
			lbl4.setPreferredSize( new Dimension(800, 2) );
			spacePane.add(lbl4);
			add(spacePane);
			String ion = info[i].getIon();
			peaks1[i] = new Peak((Vector<String>)mzAry.get(i), emass, ion);
		}
		initMass();
	}

	/**
	 * 
	 */
	public void clear()
	{
		peaks1 = null;
		massStart = 0;
		massRangeMax = 0;
		massRange = 0;
		intensityRange = INTENSITY_MAX;
	}

	/**
	 * 
	 */
	public void initMass()
	{
		massRange = -1;
		for ( int id = 0; id < numSpct; id ++ ) {
			double max = peaks1[id].compMaxMzPrecusor(precursor[id]);
			if ( massRange < max ) { massRange = max; }
		}

		// massRangeが100で割り切れる場合は+100の余裕を持つ
		if (massRange != 0.0 && (massRange % 100.0) == 0.0) {
			massRange += 100.0;
		}
		// massRangeを100単位にそろえる
		massRange = (double) Math.ceil(massRange / 100.0) * 100.0;
		
		massStart = 0;
		intensityRange = INTENSITY_MAX;
		massRangeMax = (int)massRange;

		repaint();
	}

	/**
	 * 
	 */
	public int getIntensity()
	{
		return intensityRange;
	}

	/**
	 * ピークデータ取得
	 */
	public ArrayList<String> getPeakData()
	{
		String[] param = new String[urlList.length];
		for ( int i = 0; i < urlList.length; i++ ) {
			 param[i] = "";
		}
		int siteMax = 0;
		for ( int i = 0; i < numSpct; i++ ) {
			int site = info[i].getSiteNo();
			param[site] += info[i].getID() + ",";
			if ( siteMax < site ) {
				siteMax = site;
			}
		}

		String line;
		String[] tmp;
		ArrayList resultList = new ArrayList();
		for ( int i = 0; i < siteMax + 1; i++ ) {
			if ( cnt[i] == 0 ) {
				resultList.add( null );
				continue;
			}

			// パラメータ最後尾カンマを取り除く
			param[i] = param[i].substring( 0, param[i].length() - 1 );

			// リクエストURLセット
			String typeName = MassBankCommon.CGI_TBL[MassBankCommon.CGI_TBL_NUM_TYPE][MassBankCommon.CGI_TBL_TYPE_GDATA2];
			String reqStr = baseUrl + MassBankCommon.DISPATCHER_NAME + "?type=" + typeName
				 + "&id=" + param[i] + "&site=" + Integer.toString(i);
			
			if ( reqType.equals("peak") || reqType.equals("diff") ) {
				reqStr += "&diff=";
				if ( reqType.equals("peak") ) {
					reqStr += "no";
				}
				else {
					reqStr += "yes";
				}
				reqStr += "&mz=" + paramMz.substring( 0, paramMz.length() -1 );
				reqStr += "&tol=" + paramTol.substring( 0, paramTol.length() -1 );
				reqStr += "&int=" + paramInt.substring( 0, paramInt.length() -1 );
			}

			try {
				URL url = new URL( reqStr );
				URLConnection con = url.openConnection();
				
				// レスポンス取得
				BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
				ArrayList<String> result = new ArrayList<String>();
				
				// レスポンス格納
				while ( (line = in.readLine()) != null ) {
					if ( !line.equals("") ) {
						result.add( line );
					}
				}
				resultList.add( result );
				in.close();
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return resultList;
	}

	/**
	 * Molfileデータ取得
	 */
	private Map<String,String> getMolData(HashSet<String> compoundNameList) {
		Iterator it = compoundNameList.iterator();
		String param = "";
		while ( it.hasNext() ) {
			String name = (String)it.next();
			String ename = "";
			try {
				ename = URLEncoder.encode( name, "utf-8" );
			}
			catch ( UnsupportedEncodingException e ) {
				e.printStackTrace();
			}
			param += ename + "@";
		}
		if ( !param.equals("") ) {
			param = param.substring(0, param.length()-1);
			param = "&names=" + param;
		}
		MassBankCommon mbcommon = new MassBankCommon();
		String typeName = MassBankCommon.CGI_TBL[MassBankCommon.CGI_TBL_NUM_TYPE][MassBankCommon.CGI_TBL_TYPE_GETMOL];
		ArrayList result = mbcommon.execMultiDispatcher( serverUrl, typeName, param );

		Map<String, String> map = new HashMap();
		boolean isStart = false;
		int cnt = 0;
		String key = "";
		String moldata = "";
		for ( int i = 0; i < result.size(); i++ ) {
			String temp = (String)result.get(i);
			String[] item = temp.split("\t");
			String line = item[0];
			if ( line.indexOf("---NAME:") >= 0 ) {
				if ( !key.equals("") && !map.containsKey(key) && !moldata.trim().equals("") ) {
					// Molfileデータ格納
					map.put(key, moldata);
				}
				// 次のデータのキー名
				key = line.substring(8).toLowerCase();
				moldata = "";
			}
			else {
				moldata += line + "\n";
			}
		}
		if ( !map.containsKey(key) && !moldata.trim().equals("") ) {
			map.put(key, moldata);
		}
		return map;
	}

	/*
	 * レコード情報格納データクラス
	 */
	class RecordInfo {
		private String id = "";
		private String title = "";
		private int siteNo = 0;
		private int num = 0;
		private String formula = "";
		private String exactMass = "";
		private String ion = "";
		
		/**
		 * コンストラクタ
		 */
		public RecordInfo(String id, String title, int siteNo, int num, String formula, String mass, String ion) {
			this.id = id;
			this.title = title;
			this.siteNo = siteNo;
			this.num = num;
			this.formula = formula;
			this.exactMass = mass;
			this.ion = ion;
		}
		
		/**
		 * IDをセットする
		 */
		public void setID(String val) {
			this.id = val;
		}
		/**
		 * レコードタイトルをセットする
		 */
		public void setTitle(String val) {
			this.title = val;
		}
		/**
		 * 分子式をセットする
		 */
		public void setFormula(String val) {
			this.formula = val;
		}
		/**
		 * 精密質量をセットする
		 */
		public void setExactMass(String val) {
			this.exactMass = val;
		}
		/**
		 * イオンをセットする
		 */
		public void setIon(String val) {
			this.ion = val;
		}
		/**
		 * サイト番号をセットする
		 */
		public void setSiteNo(int val) {
			this.siteNo = val;
		}
		/**
		 * 番号をセットする
		 */
		public void setNumber(int val) {
			this.num = val;
		}
		
		/**
		 * IDを取得する
		 */
		public String getID() {
			return this.id;
		}
		/**
		 * レコードタイトルを取得する
		 */
		public String getTitle() {
			return this.title;
		}
		/**
		 * サイト番号を取得する
		 */
		public int getSiteNo() {
			return this.siteNo;
		}
		/**
		 * 番号を取得する
		 */
		public int getNumber() {
			return this.num;
		}
		/**
		 * 分子式を取得する
		 */
		public String getFormula() {
			return this.formula;
		}
		/**
		 * 精密質量を取得する
		 */
		public String getExactMass() {
			return this.exactMass;
		}
		/**
		 * イオンを取得する
		 */
		public String getIon() {
			return this.ion;
		}
	}
}
