package com.commit451.gitlab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.commit451.gitlab.api.GitLabClient;
import com.commit451.gitlab.events.CloseDrawerEvent;
import com.commit451.gitlab.events.ProjectChangedEvent;
import com.commit451.gitlab.fragments.CommitsFragment;
import com.commit451.gitlab.fragments.FilesFragment;
import com.commit451.gitlab.fragments.IssuesFragment;
import com.commit451.gitlab.fragments.UsersFragment;
import com.commit451.gitlab.model.Branch;
import com.commit451.gitlab.model.Group;
import com.commit451.gitlab.model.Project;
import com.commit451.gitlab.model.User;
import com.commit451.gitlab.tools.Prefs;
import com.commit451.gitlab.tools.Repository;
import com.commit451.gitlab.tools.RetrofitHelper;
import com.commit451.gitlab.views.GitLabNavigationView;
import com.squareup.otto.Subscribe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends BaseActivity {

	@Bind(R.id.toolbar) Toolbar toolbar;
	@Bind(R.id.tabs) TabLayout tabs;
	@Bind(R.id.branch_spinner) Spinner branchSpinner;
	@Bind(R.id.drawer_layout) DrawerLayout drawerLayout;
	@Bind(R.id.navigation_view) GitLabNavigationView navigationView;
	@Bind(R.id.pager) ViewPager viewPager;

	private final AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			Repository.selectedBranch = Repository.branches.get(position);
			Prefs.setLastBranch(MainActivity.this, Repository.selectedBranch.getName());
			loadData();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) { }
	};

	EventReceiver eventReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		eventReceiver = new EventReceiver();
		GitLabApp.bus().register(eventReceiver);

		toolbar.setNavigationIcon(R.drawable.ic_menu);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				drawerLayout.openDrawer(GravityCompat.START);
			}
		});
		toolbar.inflateMenu(R.menu.main);
		toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch(item.getItemId()) {
					case R.id.action_logout:
						Prefs.setLoggedIn(MainActivity.this, false);
						startActivity(new Intent(MainActivity.this, LoginActivity.class));
						return true;
					default:
						return false;
				}
			}
		});
		
		// Workaround that forces the overflow menu
        try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		if(!Prefs.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
        }
		else {
            connect();
        }

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
		Repository.displayWidth = size.x;
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		
		// Set up the ViewPager with the sections adapter.
		viewPager.setAdapter(sectionsPagerAdapter);
		viewPager.setOffscreenPageLimit(3);
		tabs.setupWithViewPager(viewPager);
	}

	@Override
	protected void onDestroy() {
		GitLabApp.bus().unregister(eventReceiver);
		super.onDestroy();
	}

	public void hideSoftKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}
	
	@Override
	public void onBackPressed() {
		boolean handled = false;
		
		switch(viewPager.getCurrentItem()) {
			case 0:
				CommitsFragment commitsFragment = (CommitsFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":0");
				handled = commitsFragment.onBackPressed();
				break;
			case 1:
				IssuesFragment issuesFragment = (IssuesFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":1");
				handled = issuesFragment.onBackPressed();
				break;
			case 2:
				FilesFragment filesFragment = (FilesFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":2");
				handled = filesFragment.onBackPressed();
				break;
			case 3:
				UsersFragment usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":3");
				handled = usersFragment.onBackPressed();
				break;
		}
		
		if(!handled)
			finish();
	}
	
	private void loadData() {
		if(Repository.selectedProject == null)
			return;
		
		CommitsFragment commitsFragment = (CommitsFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":0");
		commitsFragment.loadData();
		
		IssuesFragment issuesFragment = (IssuesFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":1");
		issuesFragment.loadData();
		
		FilesFragment filesFragment = (FilesFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":2");
		filesFragment.loadData();
		
		UsersFragment usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":3");
		usersFragment.loadData();
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			
			switch(position) {
				case 0:
					fragment = new CommitsFragment();
					break;
				case 1:
					fragment = new IssuesFragment();
					break;
				case 2:
					fragment = new FilesFragment();
					break;
				case 3:
					fragment = new UsersFragment();
					break;
			}

			return fragment;
		}
		
		@Override
		public int getCount() {
			return 4;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch(position) {
				case 0:
					return getString(R.string.title_section1);
				case 1:
					return getString(R.string.title_section2);
				case 2:
					return getString(R.string.title_section3);
				case 3:
					return getString(R.string.title_section4);
			}
			return null;
		}
	}
	
	/* --- CONNECT --- */
	
	private ProgressDialog pd;
	
	private void connect() {
		pd = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.main_progress_dialog), true);
        GitLabClient.instance().getGroups(groupsCallback);
	}
	
	private Callback<List<Group>> groupsCallback = new Callback<List<Group>>() {
		
		@Override
		public void success(List<Group> groups, Response resp) {
			Repository.groups = new ArrayList<>(groups);

            GitLabClient.instance().getUsers(usersCallback);
		}
		
		@Override
		public void failure(RetrofitError e) {
			RetrofitHelper.printDebugInfo(MainActivity.this, e);
            GitLabClient.instance().getUsers(usersCallback);
		}
	};
	
	private Callback<List<User>> usersCallback = new Callback<List<User>>() {
		
		@Override
		public void success(List<User> users, Response resp) {
			Repository.users = new ArrayList<>(users);

            GitLabClient.instance().getProjects(projectsCallback);
		}
		
		@Override
		public void failure(RetrofitError e) {
			RetrofitHelper.printDebugInfo(MainActivity.this, e);
			
			if(pd != null && pd.isShowing())
				pd.cancel();

			Snackbar.make(getWindow().getDecorView(), getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
					.show();
		}
	};
	
	private Callback<List<Project>> projectsCallback = new Callback<List<Project>>() {
		
		@Override
		public void success(List<Project> projects, Response resp) {
			Repository.projects = new ArrayList<>(projects);

			if(Repository.projects.size() != 0) {
				if(Prefs.getLastProject(MainActivity.this).length() == 0)
					Repository.selectedProject = Repository.projects.get(0);
				else if(Repository.projects.size() > 0) {
					String lastProject = Prefs.getLastProject(MainActivity.this);

					for(Project p : Repository.projects) {
						if(p.toString().equals(lastProject))
							Repository.selectedProject = p;
					}

					if(Repository.selectedProject == null)
						Repository.selectedProject = Repository.projects.get(0);
				}
			}
			
			if(Repository.selectedProject != null)
                GitLabClient.instance().getBranches(Repository.selectedProject.getId(), branchesCallback);
            else {
				if (pd != null && pd.isShowing()) {
					pd.cancel();
				}
			}
			navigationView.setProjects(projects);
		}
		
		@Override
		public void failure(RetrofitError e) {
			RetrofitHelper.printDebugInfo(MainActivity.this, e);

            if(pd != null && pd.isShowing())
                pd.cancel();
			Snackbar.make(getWindow().getDecorView(), getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
					.show();
		}
	};
	
	private Callback<List<Branch>> branchesCallback = new Callback<List<Branch>>() {
		
		@Override
		public void success(List<Branch> branches, Response resp) {
			if(pd != null && pd.isShowing())
				pd.cancel();
			
			Repository.branches = new ArrayList<>(branches);
			Branch[] spinnerData = new Branch[Repository.branches.size()];
			int selectedBranchIndex = -1;
			
			for(int i = 0; i < Repository.branches.size(); i++)
			{
				spinnerData[i] = Repository.branches.get(i);

                if(Prefs.getLastBranch(MainActivity.this).equals(spinnerData[i].getName()))
                    selectedBranchIndex = i;
                else if(selectedBranchIndex == -1 && Repository.selectedProject != null && spinnerData[i].getName().equals(Repository.selectedProject.getDefaultBranch()))
                    selectedBranchIndex = i;
			}

			// Set up the dropdown list navigation in the action bar.
			branchSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, spinnerData));
			if (selectedBranchIndex >= 0) {
				branchSpinner.setSelection(selectedBranchIndex);
			}
			branchSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
			
			if(Repository.branches.size() == 0) {
				Repository.selectedBranch = null;
				loadData();
			}
		}
		
		@Override
		public void failure(RetrofitError e) {
			if(pd != null && pd.isShowing())
				pd.cancel();

            if(e.getResponse().getStatus() == 500) {
                Repository.selectedBranch = null;
                loadData();
                return;
            }

            RetrofitHelper.printDebugInfo(MainActivity.this, e);
			Snackbar.make(getWindow().getDecorView(), getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
					.show();
		}
	};

	private class EventReceiver {

		@Subscribe
		public void onCloseDrawerEvent(CloseDrawerEvent event) {
			drawerLayout.closeDrawers();
		}

        @Subscribe
        public void onProjectChanged(ProjectChangedEvent event) {
            GitLabClient.instance().getBranches(Repository.selectedProject.getId(), branchesCallback);
        }
	}
}
