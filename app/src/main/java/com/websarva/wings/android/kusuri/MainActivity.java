package com.websarva.wings.android.kusuri;

import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.websarva.wings.android.kusuri.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ViewBinding を使用して layout/activity_main.xml をバインド
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // BottomNavigationView のセットアップ
        BottomNavigationView navView = findViewById(R.id.nav_view);

        // 各フラグメントをトップレベルの目的地（トップレベルの画面）として設定
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        // ナビゲーションコントローラを取得
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // アクションバーとナビゲーションコントローラを連携
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // BottomNavigationView とナビゲーションコントローラを連携
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // ナビゲーションアップボタン（←）が押されたときの処理
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }



}