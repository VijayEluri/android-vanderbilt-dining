package edu.vanderbilt.vuphone.android.objects;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import edu.vanderbilt.vuphone.android.dining.R;
import edu.vanderbilt.vuphone.android.storage.Restaurant;



/**
 * Restaurant List Adaptor, handles sorting and display of the list of restaurants and partitions of the 
 * main application page
 * 
 * @author austin
 * 
 */
public class RestaurantAdapter extends BaseAdapter implements ListAdapter 
{
	
	private static final int NUM_BOOLEANS = 8;
	
	// booleans (maximum 8 boolean values, remove sort levels if need more)
	public static final int ALPHABETICAL 			= 16; // 2 ^ 4 to be independant of other sorts below
	public static final int SHOW_FAV_PART 			= 1; // 2 ^ 0
	public static final int SHOW_OPEN_PART 			= 2; // ...
	public static final int HIDE_OFF_CAMPUS 		= 4;
	public static final int HIDE_OFF_THE_CARD		= 8;

	
	// sort options for each level (maximum value 7)
	public static final int SORT_UNSORTED 				= 0;
	public static final int SORT_TIME_TO_CLOSE 			= 1;
	public static final int SORT_TIME_TO_OPEN			= 2;
	public static final int SORT_FAVORITE 				= 3;
	public static final int SORT_OPEN_CLOSED			= 4;
	public static final int SORT_NEAR_FAR				= 5;
	
	// ascending is default
	public static final int DESCENDING 				= 0x8; // the bit, 1 for true, 0 for false
	
	// sort levels, higher the level, the more recent the sort (most top level)
	public static final int []LEVEL					= { 0x100,
														0x1000,
														0x10000,
														0x100000,
														0x1000000,
														0x10000000 };
	
	/** The default sort method */
	public static final int DEFAULT_SORT = SORT_UNSORTED;
	
	// non restaurant item Ids, must be negative
	public static final long FAVORITE_PARTITION = -1;
	public static final long OPEN_PARTITION = -2;
	public static final long CLOSED_PARTITION = -3;
	public static final long OTHER_PARTITION = -4;
	
		
	private Context _context;
	
	private ArrayList<Long> _order;
	private int currentSortType;
	
	private boolean showFavIcon;
	private boolean grayClosed;
	private boolean showRestaurantType = false;
	private boolean showDistances = false;
	
	ArrayList<Double> distances;
	LocationManager locationManager;
	
	public RestaurantAdapter(Context context) {
		this(context, DEFAULT_SORT);
	}
	
	public RestaurantAdapter(Context context, int sortType) {
		_context = context;
		setSort(sortType);
		locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
	} 
	
	public RestaurantAdapter(Context context, ArrayList<Long> sortOrder, boolean showFavIcon) {
		_context = context;
		_order = sortOrder;
		currentSortType = SORT_UNSORTED;
		setShowFavIcon(showFavIcon);
		locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public RestaurantAdapter(Context context, boolean open, boolean timeUntil, boolean nearFar, boolean favorite) {
		_context = context;
		setSort(open, timeUntil, nearFar, favorite, false, false);
		locationManager = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	/** @see android.widget.Adapter#getCount() */
	public int getCount() {
		return _order.size();
	}
	
	/** @see android.widget.Adapter#getItem(int) */
	public Object getItem(int i) {
		if (getItemId(i) > 0)
			return Restaurant.get(getItemId(i)); 
		else return getItemId(i); // TODO - this should probably just return null
	}
	
	/** @see android.widget.Adapter#getItemId(int) */
	public long getItemId(int i) {
		return _order.get(i);
	}
	
	/** @see android.widget.Adapter#getView(int, View, ViewGroup) */
	public View getView(int i, View convertView, ViewGroup parent) {
		long rID = getItemId(i);
		if (rID>0) {
			
			ViewWrapper wrapper;
			// checks if convertView not initialized, or initialized to partition
			if (convertView == null || convertView.getTag()==null) {
				LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.main_list_item, parent, false);
				wrapper = new ViewWrapper(convertView);
				convertView.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper)convertView.getTag();
			}
			//Restaurant r = (Restaurant)current;
			if (getShowFavIcon()) {
				wrapper.getFavoriteView().setVisibility(View.VISIBLE);
				wrapper.getFavoriteView().setImageResource(Restaurant.favorite(rID)?
											R.drawable.star_enabled:		// favorite icon 
											R.drawable.star_gray);	// nonfavorite icon
			
			} else {
				wrapper.getFavoriteView().setVisibility(View.GONE);
			}
			wrapper.getNameView().setText(Restaurant.getName(rID));
			wrapper.getSpecialView().setText(getSpecialText(rID));
			wrapper.getSpecialRightView().setText(getSpecialRightText(rID));

			if (getGrayClosed()) {
				boolean enabled = Restaurant.getHours(rID).isOpen();
				wrapper.getNameView().setEnabled(enabled);
				wrapper.getSpecialView().setEnabled(enabled);
				wrapper.getFavoriteView().setEnabled(enabled);
				wrapper.getSpecialRightView().setEnabled(enabled);
			} else {
				wrapper.getNameView().setEnabled(true);
				wrapper.getSpecialView().setEnabled(true);
				wrapper.getFavoriteView().setEnabled(true);
				wrapper.getSpecialRightView().setEnabled(true);
			}
			
			 return convertView;
		} else {
			TextView partition;
			if (convertView == null) {
				partition = new TextView(_context);
				partition.setBackgroundResource(android.R.drawable.dark_header);
				partition.setGravity(Gravity.CENTER_VERTICAL);
				partition.setFocusable(false);
				partition.setClickable(false);
				partition.setTextSize((float) 14.0);
				partition.setTypeface(Typeface.DEFAULT_BOLD);
			} else partition = (TextView)convertView;
			switch ((int)rID) { 
			case (int)FAVORITE_PARTITION:
				partition.setText("Favorites");
				break;
			case (int)OPEN_PARTITION:
				partition.setText("Open");
				break;
			case (int)CLOSED_PARTITION:
				partition.setText("Closed");
				break;				
			case (int)OTHER_PARTITION:
				partition.setText("Other");
			}
			return partition;
			
		} 
	}
	
	public static String hoursText(long rID) {
		StringBuilder out = new StringBuilder();
		RestaurantHours rh = Restaurant.getHours(rID);
		int toOpen = rh.minutesToOpen();
		if (toOpen==0) {
			int min = rh.minutesToClose();
			if (min >= 1440)
				out.append("open"); // open for 24 hours or more
			else if (min<=60)
				out.append("open for ").append(min).append(" minutes");
			else out.append("open until ").append(rh.getNextCloseTime().toString());
		} else if (toOpen>0) {
			if (toOpen<=60)
				out.append("closed, opens in ").append(toOpen).append(" minutes");
			else out.append("closed until ").append(rh.getNextOpenTime().toString());
		} else out.append("closed"); // closed for the day
		return out.toString();
	}
	
	private String getSpecialText(long rID) {
		if (showRestaurantType) {
			return Restaurant.getType(rID) + " ";
		} else {
			return hoursText(rID) + " ";
		}
	}
	
	private String getSpecialRightText(long rID) {
		StringBuilder out = new StringBuilder();
		if (showDistances && distances != null) {
			double distance = distances.get(Restaurant.getI(rID));
			if (distance < 1000)
				out.append((int)(distance / 10 + .5) * 10).append(" ft");
			else out.append(((int)(distance / 5280 * 10 + .5)) / 10.0).append(" mi");
		}
		return out.append(" ").toString();
	}
	
	private static final int RESTAURANT = 1;
	private static final int PARTITION = 0;
	@Override
	public int getItemViewType(int i) {
		return getItemId(i)>0?RESTAURANT:PARTITION;
	}
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	@Override 
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int i) {
		return getItemViewType(i) != PARTITION;
	}
	
	
	public void setSort(boolean favorite, boolean open, boolean timeUntil, boolean nearFar, 
			boolean settingsModified, boolean sortSettingsModified) {
		currentSortType = currentSortType & ((1 << NUM_BOOLEANS) - 1);
		if (nearFar) {
			setSortAtLevel(getNextUnusedLevel(), SORT_NEAR_FAR);
		}
		if (open) {
			setSortAtLevel(getNextUnusedLevel(), SORT_OPEN_CLOSED);
		}
		if (timeUntil) {
			setSortAtLevel(getNextUnusedLevel(), SORT_TIME_TO_CLOSE);
			setSortAtLevel(getNextUnusedLevel(), SORT_TIME_TO_OPEN);
		}
		if (favorite) {
			setSortAtLevel(getNextUnusedLevel(), SORT_FAVORITE);
		} 
		if (!settingsModified) 
			setAllBoolsToDefault();
		else if (!sortSettingsModified) 
			setSortBoolsToDefault();
		else 
			setNonSettingBoolsToDefault();
		

		setSort();
	}
	
	
	/**
	 * Sets sort method for list and sorts
	 * @param sortType 
	 * 		a class constant
	 */
	public void setSort(int sortType) {
		
		if (sortType == currentSortType)
			return;
		currentSortType = sortType;
		_order = Restaurant.copyIDs();
		
		if (getHideOffCampus()) {
			for (int i = _order.size() - 1; i >= 0; i--)
				if (Restaurant.offCampus(_order.get(i)))
					_order.remove(i);
		}
		if (getHideOffTheCard()) {
			for (int i = _order.size() - 1; i >= 0; i--)
				if (!Restaurant.onTheCard(_order.get(i)))
					_order.remove(i);
		}
		
		if ((sortType & ALPHABETICAL) > 0)
			sort(_order, ALPHABETICAL);
		
		for(int level = 0; level < LEVEL.length; level++) {
			int sort = getSortAtLevel(level);
			switch (sort & 0x7) {
			case SORT_FAVORITE:
			case SORT_OPEN_CLOSED:
			case SORT_NEAR_FAR:
				sort(_order, sort);
				break;
			case SORT_TIME_TO_CLOSE:
				sort(_order.subList(0, firstClosed()), sort);
				break;
			case SORT_TIME_TO_OPEN:
				sort(_order.subList(firstClosed(), _order.size()), sort);
				break;
				
			}
		}
		
		// NOW BEGIN ADDING PARTITIONS
		
		boolean favPart = (sortType & SHOW_FAV_PART) > 0;
		int nonFav = -1;
		boolean openPart = (sortType & SHOW_OPEN_PART) > 0;
		int closed = -1;
		if (favPart) {
			nonFav = firstNonFavorite();
			if (openPart)
				closed = firstClosed(nonFav);
		} else {
			if (openPart)
				closed = firstClosed();
		}
		
		if (openPart && closed != -1)
			_order.add(closed, CLOSED_PARTITION);
		if (favPart) {
			if (openPart) {
				if (nonFav != closed && nonFav != -1) {
					_order.add(nonFav, OPEN_PARTITION);
				}
			} else {
				if (nonFav != -1)
					_order.add(nonFav, OTHER_PARTITION);
			}
		}
		
		if (favPart) {
			if (nonFav != 0)
				_order.add(0, FAVORITE_PARTITION);
		} else {
			if (openPart && closed != 0)
				_order.add(0, OPEN_PARTITION);
		}
		
	}
	
	public void setSort() {
		int oldSort = currentSortType;
		currentSortType = SORT_UNSORTED;
		setSort(oldSort);
	}
	
	public int getSortType() {
		return currentSortType;
	}
	
	public void setNonSettingBoolsToDefault() {
		currentSortType = currentSortType | ALPHABETICAL;
		if (indexOf(SORT_FAVORITE) != -1) 
			currentSortType |= SHOW_FAV_PART;
		else currentSortType &= ~SHOW_FAV_PART;
		if (indexOf(SORT_OPEN_CLOSED) != -1) 
			currentSortType |= SHOW_OPEN_PART;
		else currentSortType &= ~SHOW_OPEN_PART;
	}
	
	// settings that depend on the sort
	public void setSortBoolsToDefault() {
		setNonSettingBoolsToDefault();
		setGrayClosed(true);
		setShowFavIcon(indexOf(SORT_FAVORITE) == -1);
	}
	
	public void setAllBoolsToDefault() {
		setSortBoolsToDefault();
		setShowDistances(false);
		setShowRestaurantType(false);
		setHideOffCampus(false);
		setHideOffTheCard(false);
	}
	
	public void setShowFavIcon(boolean show) {
		showFavIcon = show;
	}
	
	public boolean getShowFavIcon() {
		return showFavIcon;
	}
	
	public void setGrayClosed(boolean gray) {
		grayClosed = gray;
	}
	
	public boolean getGrayClosed() {
		return grayClosed;
	}
	
	public void setShowRestaurantType(boolean show) {
		showRestaurantType = show;
	}
	
	public boolean getShowRestaurantType() {
		return showRestaurantType;
	}
	
	public void setShowDistances(boolean show) {
		showDistances = show;
	}
	
	public boolean getShowDistances() {
		return showDistances;
	}
	
	public void setHideOffCampus(boolean hide) {
		if (hide)
			currentSortType |= HIDE_OFF_CAMPUS;
		else currentSortType &= ~HIDE_OFF_CAMPUS;
	}
	
	public boolean getHideOffCampus() {
		return (currentSortType & HIDE_OFF_CAMPUS) > 0;
	}
	
	public void setHideOffTheCard(boolean hide) {
		if (hide)
			currentSortType |= HIDE_OFF_THE_CARD;
		else currentSortType &= ~HIDE_OFF_THE_CARD;
	}
	
	public boolean getHideOffTheCard() {
		return (currentSortType & HIDE_OFF_THE_CARD) > 0;
	}
	
	
	
	/**	
	 * @return 
	 * the underlying array of Restaurant row ids and partitions,
	 * modifying this will change the actual sort of the RestaurantAdapter
	 */
	public ArrayList<Long> getSortOrder() {
		return _order;
	}
	
	
	/**
	 * @param order
	 * the new order of display elements, add a partition using the 
	 * class constants as row ids
	 */
	public void setSortOrder(ArrayList<Long> order) {
		_order = order;
	}
	
	
	/**
	 * finds the next usable level, and compacts the rest if out of room
	 * @return
	 * 	the index of an unused level on top of all others
	 */
	public int getNextUnusedLevel() {
		for (int level = LEVEL.length-1; level >=0; level--) 
			if (getSortAtLevel(level) != SORT_UNSORTED)
				if (level + 1 < LEVEL.length)
					return level + 1;
				else break;
		if (getSortAtLevel(0) == SORT_UNSORTED)
			return 0;
		if (compactLevels())
			return getNextUnusedLevel();
		return -1;
	}
	
	 
	/** sets the indicated level to the indicated sort
	 * pre: level < LEVEL.length, 0<=sort<16
	 * @param level
	 * index of level to set
	 * @param sort
	 * class constant level sort to set
	 */
	public void setSortAtLevel(int level, int sort) {
		if (level>=LEVEL.length || level<0)
			throw new RuntimeException("level bad: " + level);
		currentSortType &= ~(LEVEL[level] * 0xF);
		currentSortType |= (LEVEL[level] * sort);
	}
	
	public int getSortAtLevel(int level) {
		return (currentSortType & (LEVEL[level] * 0xF)) >> (level * 4 + NUM_BOOLEANS);
	}
	
	public int indexOf(int sort) {
		for (int i = 0; i < LEVEL.length; i++)
			if (getSortAtLevel(i) == sort)
				return i;
		return -1;
	}
	
	public boolean insert(int i, int sort) {
		int sortHere = getSortAtLevel(i);
		if (sortHere != SORT_UNSORTED) {
			if (i+1 >= LEVEL.length || !insert(i+1, sortHere))
				return false;
		}
		setSortAtLevel(i, sort);
		return true;
	}
	
	private boolean compactLevels() {
		boolean changed = false;
		int shiftsRemain = LEVEL.length - 1;
		for (int level = 0; level < LEVEL.length - 1; level++) {
			while ((currentSortType & (LEVEL[level] * 0xF)) == SORT_UNSORTED && shiftsRemain-->0) {
				int onesRightOfLevel = (1 << (level * 4 + NUM_BOOLEANS)) - 1;
				int newLeft = (currentSortType & ~onesRightOfLevel) >> 4; 
				if (newLeft == 0)
					return changed;
				else changed = true;
				currentSortType &= onesRightOfLevel;
				currentSortType |= newLeft;
			}
			shiftsRemain--;
		}
		return changed;
	}
	
	
	// returns the first instance of a non favorite element in the sort, -1 if all are favorites
	// useful in in lists sorted by favorites
	private int firstNonFavorite() {
		for (int i = 0; i<_order.size(); i++)
			if (!Restaurant.favorite(_order.get(i)))
				return i;
		return -1;
	}
	private int firstClosed(int start) {
		for (int i = start; i<_order.size(); i++)
			if (!Restaurant.getHours(_order.get(i)).isOpen())
				return i;
		return -1;			
	}
	private int firstClosed() {
		return firstClosed(0);
	}
	
	public boolean refreshDistances() {
		Location here = getCurrentLocation();
//		Location here = new Location("test"); // use these to fake a position in the middle of campus
//		here.setLatitude(36.143299); 
//		here.setLongitude(-86.802464);
		if (here == null) 
			return false;
		Location location = new Location("");
		ArrayList<Long> IDs = Restaurant.getIDs();
		distances = new ArrayList<Double>();
		distances.ensureCapacity(IDs.size());
		for (int i = 0; i < IDs.size(); i++) {
			location.setLatitude(Restaurant.getLat(IDs.get(i)) * 1.0E-6);
			location.setLongitude(Restaurant.getLon(IDs.get(i)) * 1.0E-6);
			distances.add(here.distanceTo(location) *  3.2808399); // in feet
		}
		return true;
	}
	
	private Location getCurrentLocation() {
		Criteria needed = new Criteria();
		needed.setAccuracy(Criteria.ACCURACY_FINE);
		needed.setAltitudeRequired(false);
		needed.setBearingRequired(false);
		needed.setSpeedRequired(false);
		return locationManager.getLastKnownLocation(locationManager.getBestProvider(needed, true));
	}
	
	
	
	//implementation of merge sort
	private void sort(List<Long> toSort, int sortType) {
		if (toSort.size()<=1)
			return;
		
		int center = toSort.size()/2;
		ArrayList<Long> left = new ArrayList<Long>();
		left.addAll(toSort.subList(0, center));
		
		ArrayList<Long> right = new ArrayList<Long>();
		right.addAll(toSort.subList(center, toSort.size()));
		
		sort(left, sortType);
		sort(right, sortType);
		
		int li = 0, ri = 0, i=0;
		while (li < left.size() && ri < right.size()) {
			if (compare(left.get(li), right.get(ri), sortType))
				toSort.set(i++, left.get(li++));
			else toSort.set(i++, right.get(ri++));
		}
		
		for (;li < left.size();)
			toSort.set(i++, left.get(li++));
		for (;ri < right.size();)
			toSort.set(i++, right.get(ri++));
	}
	
	public int currentSortCompareCached = SORT_UNSORTED;
	@SuppressWarnings("unchecked")
	ArrayList compareCache;
	
	// compare method for merge, 'less or equal'
	private boolean compare(long first, long second, int sortType) {
		if (sortType == ALPHABETICAL)
			return Restaurant.getName(first).compareToIgnoreCase(Restaurant.getName(second)) <= 0; 
		
		if (currentSortCompareCached != sortType)
			createCompareCache(sortType);
		
		switch (sortType & 0x7) {
		case SORT_FAVORITE:
		case SORT_OPEN_CLOSED:
			if ((sortType & DESCENDING) == 0)
				return ((Boolean)compareCache.get(Restaurant.getI(first))) || !((Boolean)compareCache.get(Restaurant.getI(second)));
			else
				return ((Boolean)compareCache.get(Restaurant.getI(second))) || !((Boolean)compareCache.get(Restaurant.getI(first)));
		case SORT_TIME_TO_CLOSE:
			if ((sortType & DESCENDING) == 0)
				return ((Integer)compareCache.get(Restaurant.getI(first))) <= ((Integer)compareCache.get(Restaurant.getI(second)));
			else
				return ((Integer)compareCache.get(Restaurant.getI(first))) >= ((Integer)compareCache.get(Restaurant.getI(second)));
		case SORT_TIME_TO_OPEN:
		{
			int fm = ((Integer)compareCache.get(Restaurant.getI(first))), 
					sm = ((Integer)compareCache.get(Restaurant.getI(second)));
			if ((sortType & DESCENDING) == 0) {
				if (sm == -1)
					return true;
				else return fm <= sm && fm != -1;
			} else {
				if (fm == -1)
					return true;
				else return fm >= sm && sm != -1;
			}
		}
		case SORT_NEAR_FAR:
			if (distances == null)
				return true;
			if ((sortType & DESCENDING) == 0)
				return distances.get(Restaurant.getI(first)) <= distances.get(Restaurant.getI(second));
			else 
				return distances.get(Restaurant.getI(first)) >= distances.get(Restaurant.getI(second));
		default:
			return true;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createCompareCache(int sortType) {
		ArrayList<Long> IDs = Restaurant.getIDs();
		compareCache = new ArrayList<Void>();
		compareCache.ensureCapacity(IDs.size());
		switch (sortType & 0x7) {
		case SORT_FAVORITE:
			for (int i = 0; i<IDs.size(); i++)
				compareCache.add(Restaurant.favorite(IDs.get(i)));
			break;
		case SORT_OPEN_CLOSED:
			for (int i = 0; i<IDs.size(); i++)
				compareCache.add(Restaurant.getHours(IDs.get(i)).isOpen());
			break;
		case SORT_TIME_TO_CLOSE:
			for (int i = 0; i<IDs.size(); i++)
				compareCache.add(Restaurant.getHours(IDs.get(i)).minutesToClose());
			break;
		case SORT_TIME_TO_OPEN:
			for (int i = 0; i<IDs.size(); i++)
				compareCache.add(Restaurant.getHours(IDs.get(i)).minutesToOpen());
			break;
		}
		currentSortCompareCached = sortType;
	}

	private class ViewWrapper {
		private View _base;
		private ImageView _favorite;
		private TextView _name;
		private TextView _special;
		private TextView _specialRight;
		
		public ViewWrapper(View base) {
			_base = base;
		}
		
		public ImageView getFavoriteView() {
			if (_favorite == null) 
				_favorite = (ImageView)_base.findViewById(R.mainListItem.favoriteIcon);
			return _favorite;
		}
		
		public TextView getNameView() {
			if (_name == null) 
				_name=(TextView)_base.findViewById(R.mainListItem.name);
			return _name;
		}
		
		public TextView getSpecialView() {
			if (_special == null)
				_special = (TextView)_base.findViewById(R.mainListItem.specialText);
			return _special;
		}
		
		public TextView getSpecialRightView() {
			if (_specialRight == null)
				_specialRight = (TextView)_base.findViewById(R.mainListItem.specialTextRight);
			return _specialRight; 
		}
	}

}