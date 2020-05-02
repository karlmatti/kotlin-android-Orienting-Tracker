package ee.taltech.kamatt.sportsmap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.kamatt.sportsmap.db.model.GpsSession
import ee.taltech.kamatt.sportsmap.db.repository.GpsSessionRepository
import kotlinx.android.synthetic.main.recycler_row_session.view.*

class DataRecyclerViewAdapterSessions(
    private val context: Context,
    private val repo: GpsSessionRepository,
    private val userId: Int
) : RecyclerView.Adapter<DataRecyclerViewAdapterSessions.ViewHolder>() {

    var dataSet: MutableList<GpsSession> =
        repo.getSessionsByUserId(userId) as MutableList<GpsSession>

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowView = inflater.inflate(R.layout.recycler_row_session, parent, false)

        return ViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = dataSet.get(position)
        holder.itemView.textViewSessionName.text = session.name
        holder.itemView.textViewDescription.text = session.description
        holder.itemView.textViewRecordedAt.text = session.recordedAt
        holder.itemView.textViewDurationOverall.text = session.duration
        holder.itemView.textViewDistanceOverall.text = session.distance.toInt().toString()
        holder.itemView.textViewTempoOverall.text = session.speed
        holder.itemView.buttonDeleteSession.setOnClickListener {
            val theRemovedItem: GpsSession = dataSet.get(position)
            // remove your item from data base
            MainActivity.deleteSessionFromDb(theRemovedItem)
            dataSet.removeAt(position) // remove the item from list

            notifyItemRemoved(position) // notify the adapter about the removed item
        }
        holder.itemView.buttonEditSession.setOnClickListener {
            val theEditedItem: GpsSession = dataSet[position]

            if (context is MainActivity) {
                context.startEditingSession(theEditedItem)
            }

        }

        holder.itemView.buttonLoadSession.setOnClickListener {
            val theLoadedItem: GpsSession = dataSet[position]
            if (context is MainActivity) {
                context.loadSession(theLoadedItem.id)
            }
        }

    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


}
