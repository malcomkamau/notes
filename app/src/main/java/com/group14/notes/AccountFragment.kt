package com.group14.notes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.group14.notes.auth.SessionManager
import com.group14.notes.util.openLogin

class AccountFragment : Fragment(R.layout.fragment_account) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Application context is used inside SessionManager; the Activity context is only used to navigate.
        val session = SessionManager(requireContext())
        view.findViewById<TextView>(R.id.tvUserName).text = session.userName
        view.findViewById<TextView>(R.id.tvUserEmail).text = session.userEmail

        // Explicit intent: account -> settings
        view.findViewById<View>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            session.clear()
            requireContext().openLogin()
            requireActivity().finish()
        }
    }
}
