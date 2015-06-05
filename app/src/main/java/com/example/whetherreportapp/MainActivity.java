package com.example.whetherreportapp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.DoubleBuffer;
import java.sql.Ref;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.whetherreportapp.common.MyDbManager;
import com.example.whetherreportapp.common.Report;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener
{
	String TAG = "**MainActivity";
	
	ProgressDialog progressdialog;
	
	LocationManager locationManager ;
	
	TextView textViewCity,textViewCountry;
	
	ListView listViewReport,listViewDates;
	
	ArrayList<Report> dataList;
	
	MyDbManager myDbManager;
	
	LinearLayout linLayReportDetailsContainer,linLayDateListContainer;
	
	List<String> datesList = null;
	
	Button btnRefresh;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		myDbManager = new MyDbManager(this);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		initModal();
		initUI();
	}
	
	private void initModal()
	{
		try 
		{
			//getCurrentLocation();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	private void initUI()
	{
		try 
		{
			
			linLayDateListContainer = (LinearLayout)findViewById(R.id.id_lin_lay_date_list_container);
			linLayReportDetailsContainer = (LinearLayout)findViewById(R.id.id_lin_lay_report_details_container);
			
			textViewCity = (TextView) findViewById(R.id.id_txt_city);
			textViewCountry = (TextView) findViewById(R.id.id_txt_country);
			listViewReport = (ListView) findViewById(R.id.id_list_view);
			listViewDates = (ListView) findViewById(R.id.id_list__view_dates);
			
			btnRefresh = (Button)findViewById(R.id.id_btn_refresh);
			btnRefresh.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					getCurrentLocation();
				}
			});
			
			displayDateList();
			//displayDataOnUI();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onBackPressed() 
	{
		if(linLayDateListContainer.getVisibility() == View.VISIBLE )
		{
			finish();
		}
		else
		{
			linLayDateListContainer.setVisibility(View.VISIBLE);
			linLayReportDetailsContainer.setVisibility(View.GONE);
			btnRefresh.setVisibility(View.VISIBLE);
		}
	}
	
	private void getCurrentLocation()
	{
		try 
		{
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if(locationManager != null)
			{
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 10, 0, this );
				//**create Dialog
				progressdialog = new ProgressDialog(this);
		        progressdialog.setMessage("Fetching The Location...");
		        progressdialog.show();   
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private void getWhetherReport(double latti, double longi)
	{
		try 
		{
			new GetReportTask(latti, longi).execute();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		if( progressdialog != null )
		{
			progressdialog.dismiss();
		}
	}
	
	private List<String> readDatesListFromDB()
	{
		List<String> requiredList = null;
		try 
		{
			//**create Table if not exist
			String createTblQuery = "CREATE TABLE IF NOT EXISTS TBL_REPORT(id INTEGER PRIMARY KEY AUTOINCREMENT,DATE TEXT,data TEXT, UNIQUE(DATE) ON CONFLICT REPLACE );";
			myDbManager.executeDDLQuery(createTblQuery);
			
			String fetchQuery = "SELECT * FROM TBL_REPORT;";
			Cursor cursor = myDbManager.executeRawQuery(fetchQuery);
			if(cursor != null )
			{
				if(cursor.moveToFirst())
				{
					requiredList = new ArrayList<String>();
					do 
					{
						String dateStr = cursor.getString(cursor.getColumnIndex("DATE"));
						if(dateStr != null )
						{
							Log.v(TAG, "readDatesListFromDB() dateStr: "+dateStr );
							requiredList.add(dateStr);
						}
						
					} while (cursor.moveToNext());
				}
				
				cursor.close();
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return requiredList;
	}
	
	private String readReportFromDB(String date)
	{
		try 
		{
			//**create Table if not exist
			String createTblQuery = "CREATE TABLE IF NOT EXISTS TBL_REPORT(id INTEGER PRIMARY KEY AUTOINCREMENT,DATE TEXT,data TEXT, UNIQUE(DATE) ON CONFLICT REPLACE );";
			myDbManager.executeDDLQuery(createTblQuery);
			
			String fetchQuery = "SELECT * FROM TBL_REPORT WHERE DATE='"+date+"';";
			Cursor cursor = myDbManager.executeRawQuery(fetchQuery);
			if(cursor != null )
			{
				if(cursor.moveToFirst())
				{
					String dataStr = cursor.getString(cursor.getColumnIndex("data"));
					if(dataStr != null )
					{
						Log.v(TAG, "readReportFromDB() dataStr: "+dataStr );
						return dataStr;
					}
				}
				
				cursor.close();
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private void storeDataInDB(String result)
	{
		try 
		{
			//**create Table if not exist
			String createTblQuery = "CREATE TABLE IF NOT EXISTS TBL_REPORT(id INTEGER PRIMARY KEY AUTOINCREMENT,DATE TEXT,data TEXT, UNIQUE(DATE) ON CONFLICT REPLACE );";
			myDbManager.executeDDLQuery(createTblQuery);
			
			//**Delete Previous
			String deleteQuery = "DELETE FROM TBL_REPORT WHERE DATE <= date('now','-10 day');";
			myDbManager.executeDDLQuery(deleteQuery);

			//**Insert
			myDbManager.insertData("TBL_REPORT", result,getCurrentDate());
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private String getCurrentDate()
	{
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
		String currentDateStr = formater.format(new Date());
		Log.v(TAG, "getCurrentDate() currentDateStr: "+currentDateStr );
		return currentDateStr;
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		Log.v(TAG, "onLocationChanged() lat:"+location.getLatitude() + " lon:"+location.getLongitude() );
		if( progressdialog != null )
		{
			progressdialog.dismiss();
		}
		
		if( locationManager != null )
		{
			locationManager.removeUpdates(this);
		}
		
		if( location != null )
		{
			getWhetherReport(location.getLatitude(),location.getLongitude());
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		Log.v(TAG, "onStatusChanged()");

	}

	@Override
	public void onProviderEnabled(String provider) 
	{
		Log.v(TAG, "onProviderEnabled()");

	}

	@Override
	public void onProviderDisabled(String provider) 
	{
		Log.v(TAG, "onProviderDisabled()");

	}
	
	private void displayDateList()
	{
		try 
		{
			linLayDateListContainer.setVisibility(View.VISIBLE);
			linLayReportDetailsContainer.setVisibility(View.GONE);
			btnRefresh.setVisibility(View.VISIBLE);
			
			datesList = readDatesListFromDB();
			if( datesList != null && datesList.size() > 0 )
			{
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
			              android.R.layout.simple_list_item_1, android.R.id.text1, datesList);
				listViewDates.setAdapter(adapter);
				listViewDates.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) 
					{
						String selectedDateStr = datesList.get(position);
						displayDataOnUI(selectedDateStr);
					}
				});
			}
			else
			{
				Toast.makeText(this,"No Data Available!", 1000).show();
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private void displayDataOnUI(String date)
	{
		try 
		{
			linLayDateListContainer.setVisibility(View.GONE);
			linLayReportDetailsContainer.setVisibility(View.VISIBLE);
			btnRefresh.setVisibility(View.GONE);

			
			String dataRes = readReportFromDB(date);
			
			if( dataRes != null && !dataRes.equals(""))
			{
				JSONObject responseJson = new JSONObject(dataRes);
				
				//**City
				JSONObject cityJson = responseJson.getJSONObject("city");
				Log.v(TAG,"displayDataOnUI() cityJson:"+cityJson);
				
				textViewCity.setText(cityJson.getString("name"));
				
				//**country
				textViewCountry.setText(cityJson.getString("country"));
				
				
				JSONArray jsonArray = responseJson.getJSONArray("list");
				
				if( jsonArray != null )
				{
					dataList = new ArrayList<Report>();
					
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONObject dataJson = jsonArray.getJSONObject(i);
						JSONArray whJsArray = dataJson.getJSONArray("weather");
						if( whJsArray != null )
						{
							JSONObject whJson = whJsArray.getJSONObject(0);
							
							Report report = new Report(whJson.getString("main"),
									whJson.getString("description"));
							dataList.add(report);
							
						}
					}
					
					//**Display ListView
					listViewReport.setAdapter(new CustomAdaptor(MainActivity.this));
				}
			}
			else
			{
				Toast.makeText(this,"No Data Available!", 1000).show();
			}
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	
	//******************************************************************
	public class GetReportTask extends AsyncTask<Void, Void, String>
	{
		 //"http://api.openweathermap.org/data/2.5/forecast/daily?lat="+lat+"&lon="+lng+"&cnt=10&mode=json";
		double latti;
		double longi;
		public GetReportTask(double latti, double longi)
		{
			this.latti = latti;
			this.longi = longi;
		}
		
		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			//**create Dialog
			progressdialog = new ProgressDialog(MainActivity.this);
	        progressdialog.setMessage("Fetching Whether Report...");
	        progressdialog.show(); 
		}
		
		@Override
		protected String doInBackground(Void... params)
		{
			try 
			{
				String url = "http://api.openweathermap.org/data/2.5/forecast/daily?lat="+latti+"&lon="+longi+"&cnt=10&mode=json";
				
				DefaultHttpClient httpClient = new DefaultHttpClient();
	            HttpPost httpPost = new HttpPost(url);
	 
	            HttpResponse httpResponse = httpClient.execute(httpPost);
	            HttpEntity httpEntity = httpResponse.getEntity();
	            InputStream is = httpEntity.getContent();
	            
	            BufferedReader reader = new BufferedReader(new InputStreamReader(
	                    is, "UTF-8"), 8);
	            StringBuilder sb = new StringBuilder();
	            String line = null;
	            while ((line = reader.readLine()) != null) {
	                sb.append( line );
	            }
	            is.close();
	            
	            Log.v(TAG, "doInBackground() Response: "+sb.toString() );
	            
	            return sb.toString();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) 
		{
			super.onPostExecute(result);
			try {
				if(progressdialog != null && progressdialog.isShowing() )
				{
					progressdialog.dismiss();
				}
				
				if( result != null )
				{
					storeDataInDB(result);
					displayDateList();
					//displayDataOnUI();
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		
	}
	
	
	//*********************************************************************
	
	
	//***********************
	public class CustomAdaptor extends ArrayAdapter<Report>
	{
		LayoutInflater inflater;
		
		public CustomAdaptor(Context context) 
		{
			super(context, R.layout.lay_list_row);
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() 
		{
			return dataList.size();
		}
		
		@Override
		public Report getItem(int position) 
		{
			return dataList.get(position);
		}
		
		@Override
		public int getPosition(Report item) 
		{
			return super.getPosition(item);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			if( convertView == null )
			{
				convertView = inflater.inflate(R.layout.lay_list_row, parent,false);
			}
			
			Report report = dataList.get(position);
			
			TextView tvTitle = (TextView)convertView.findViewById(R.id.id_txt_title);
			ImageView imgIcon = (ImageView)convertView.findViewById(R.id.id_img_icon);
			tvTitle.setText(report.getDescription());
			
			String type = report.getTitle();
			if(type.equalsIgnoreCase("Clear"))
			{
				imgIcon.setBackgroundDrawable(MainActivity.this.getResources().getDrawable(R.drawable.sunny));
			}
			else if(type.equalsIgnoreCase("Rain"))
			{
				imgIcon.setBackgroundDrawable(MainActivity.this.getResources().getDrawable(R.drawable.rain));
			}
			else if(type.equalsIgnoreCase("Cloudy"))
			{
				imgIcon.setBackgroundDrawable(MainActivity.this.getResources().getDrawable(R.drawable.cloudy));
			}
			else
			{
				imgIcon.setBackgroundDrawable(MainActivity.this.getResources().getDrawable(R.drawable.bkn_default));
			}
			
			return convertView;
		}
		
	}
	
	//*****************************
	
}
